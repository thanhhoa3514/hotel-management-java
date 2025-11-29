package com.hotelmanagement.quanlikhachsan.services.reservation;

import com.hotelmanagement.quanlikhachsan.dto.request.reservation.ReservationRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.reservation.ReservationResponse;
import com.hotelmanagement.quanlikhachsan.exception.ErrorDefinition;
import com.hotelmanagement.quanlikhachsan.mapper.ReservationMapper;
import com.hotelmanagement.quanlikhachsan.model.guest.Guest;
import com.hotelmanagement.quanlikhachsan.model.reservation.Reservation;
import com.hotelmanagement.quanlikhachsan.model.reservation.ReservationRoom;
import com.hotelmanagement.quanlikhachsan.model.reservation.ReservationStatus;
import com.hotelmanagement.quanlikhachsan.model.room.Room;
import com.hotelmanagement.quanlikhachsan.repository.GuestRepository;
import com.hotelmanagement.quanlikhachsan.repository.ReservationRepository;
import com.hotelmanagement.quanlikhachsan.repository.ReservationRoomRepository;
import com.hotelmanagement.quanlikhachsan.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReservationServiceImpl implements IReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final GuestRepository guestRepository;
    private final RoomRepository roomRepository;
    private final ReservationMapper reservationMapper;

    @Override
    public ReservationResponse createReservation(ReservationRequest request) {
        log.debug("Creating reservation for keycloakUserId: {}", request.keycloakUserId());

        // Validate dates
        validateDateRange(request.checkIn(), request.checkOut());

        // Find guest by keycloakUserId
        Guest guest = guestRepository.findByKeycloakUserId(request.keycloakUserId())
                .orElseThrow(() -> ErrorDefinition.GUEST_NOT_FOUND.toAppError()
                        .withDetail("keycloakUserId", request.keycloakUserId()));

        // Validate room availability
        List<Room> rooms = validateAndGetRooms(request.roomIds(), request.checkIn(), request.checkOut(), null);

        // Calculate total amount
        BigDecimal totalAmount = calculateTotalAmount(rooms, request.checkIn(), request.checkOut());

        // Create reservation
        Reservation reservation = Reservation.builder()
                .guest(guest)
                .checkIn(request.checkIn())
                .checkOut(request.checkOut())
                .status(request.status() != null ? request.status() : ReservationStatus.PENDING)
                .totalAmount(totalAmount)
                .reservationRooms(new ArrayList<>())
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        // Create reservation-room relationships
        for (Room room : rooms) {
            ReservationRoom reservationRoom = ReservationRoom.builder()
                    .reservation(savedReservation)
                    .room(room)
                    .build();
            savedReservation.getReservationRooms().add(reservationRoom);
        }

        savedReservation = reservationRepository.save(savedReservation);

        log.info("Reservation created successfully with ID: {}", savedReservation.getId());
        return reservationMapper.toResponse(savedReservation);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(UUID id) {
        log.debug("Fetching reservation with ID: {}", id);
        Reservation reservation = findReservationById(id);
        return reservationMapper.toResponse(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllReservations() {
        log.debug("Fetching all reservations");
        return reservationRepository.findAll().stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByGuestId(UUID guestId) {
        log.debug("Fetching reservations for guest ID: {}", guestId);
        return reservationRepository.findByKeycloakUserId(guestId).stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByStatus(UUID statusId) {
        // Note: This implementation assumes statusId maps to ReservationStatus enum
        // Adjust if using database lookup table for statuses
        log.debug("Fetching reservations by status");
        return reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() != null)
                .map(reservationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByDateRange(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching reservations between {} and {}", startDate, endDate);
        return reservationRepository.findByDateRange(startDate, endDate).stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    @Override
    public ReservationResponse updateReservation(UUID id, ReservationRequest request) {
        log.debug("Updating reservation with ID: {}", id);

        Reservation reservation = findReservationById(id);

        // Cannot modify cancelled or checked out reservations
        if (reservation.getStatus() == ReservationStatus.CANCELLED ||
                reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw ErrorDefinition.RESERVATION_CANNOT_MODIFY.toAppError()
                    .withDetail("status", reservation.getStatus());
        }

        // Validate dates if changed
        if (!request.checkIn().equals(reservation.getCheckIn()) ||
                !request.checkOut().equals(reservation.getCheckOut())) {
            validateDateRange(request.checkIn(), request.checkOut());
        }

        // Validate room availability (excluding current reservation)
        List<Room> rooms = validateAndGetRooms(request.roomIds(), request.checkIn(), request.checkOut(), id);

        // Update reservation details
        reservation.setCheckIn(request.checkIn());
        reservation.setCheckOut(request.checkOut());
        if (request.status() != null) {
            reservation.setStatus(request.status());
        }

        // Recalculate total amount
        reservation.setTotalAmount(calculateTotalAmount(rooms, request.checkIn(), request.checkOut()));

        // Update rooms - clear existing and add new
        reservation.getReservationRooms().clear();
        for (Room room : rooms) {
            ReservationRoom reservationRoom = ReservationRoom.builder()
                    .reservation(reservation)
                    .room(room)
                    .build();
            reservation.getReservationRooms().add(reservationRoom);
        }

        Reservation updatedReservation = reservationRepository.save(reservation);
        log.info("Reservation updated successfully with ID: {}", updatedReservation.getId());
        return reservationMapper.toResponse(updatedReservation);
    }

    @Override
    public ReservationResponse updateReservationStatus(UUID id, UUID statusId) {
        // Note: This implementation uses enum. Adjust if using database lookup table.
        log.debug("Updating reservation status for ID: {}", id);
        Reservation reservation = findReservationById(id);
        // Status update logic would go here if using lookup table
        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Override
    public ReservationResponse addRoomToReservation(UUID reservationId, UUID roomId) {
        log.debug("Adding room {} to reservation {}", roomId, reservationId);

        Reservation reservation = findReservationById(reservationId);

        // Cannot modify cancelled or checked out reservations
        if (reservation.getStatus() == ReservationStatus.CANCELLED ||
                reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw ErrorDefinition.RESERVATION_CANNOT_MODIFY.toAppError();
        }

        Room room = roomRepository.findById(roomId.toString())
                .orElseThrow(() -> ErrorDefinition.ROOM_NOT_FOUND.toAppError()
                        .withDetail("roomId", roomId));

        // Check room availability
        if (reservationRoomRepository.hasConflictingReservationExcluding(
                room.getId(), reservation.getCheckIn(), reservation.getCheckOut(), reservationId)) {
            throw ErrorDefinition.ROOM_NOT_AVAILABLE.toAppError()
                    .withDetail("roomId", roomId);
        }

        ReservationRoom reservationRoom = ReservationRoom.builder()
                .reservation(reservation)
                .room(room)
                .build();
        reservation.getReservationRooms().add(reservationRoom);

        // Recalculate total
        List<Room> allRooms = reservation.getReservationRooms().stream()
                .map(ReservationRoom::getRoom)
                .toList();
        reservation.setTotalAmount(calculateTotalAmount(allRooms, reservation.getCheckIn(), reservation.getCheckOut()));

        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Override
    public void removeRoomFromReservation(UUID reservationId, UUID roomId) {
        log.debug("Removing room {} from reservation {}", roomId, reservationId);

        Reservation reservation = findReservationById(reservationId);

        // Cannot modify cancelled or checked out reservations
        if (reservation.getStatus() == ReservationStatus.CANCELLED ||
                reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw ErrorDefinition.RESERVATION_CANNOT_MODIFY.toAppError();
        }

        reservation.getReservationRooms().removeIf(rr -> rr.getRoom().getId().equals(roomId.toString()));

        // Recalculate total
        List<Room> remainingRooms = reservation.getReservationRooms().stream()
                .map(ReservationRoom::getRoom)
                .toList();
        reservation.setTotalAmount(
                calculateTotalAmount(remainingRooms, reservation.getCheckIn(), reservation.getCheckOut()));

        reservationRepository.save(reservation);
    }

    @Override
    public ReservationResponse addServiceToReservation(UUID reservationId, UUID serviceId, Integer quantity) {
        // TODO: Implement when ReservationService entity is added
        log.debug("Adding service {} to reservation {}", serviceId, reservationId);
        Reservation reservation = findReservationById(reservationId);
        return reservationMapper.toResponse(reservation);
    }

    @Override
    public void removeServiceFromReservation(UUID reservationId, UUID serviceId) {
        // TODO: Implement when ReservationService entity is added
        log.debug("Removing service {} from reservation {}", serviceId, reservationId);
    }

    @Override
    public ReservationResponse checkIn(UUID id) {
        log.debug("Checking in reservation with ID: {}", id);

        Reservation reservation = findReservationById(id);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw ErrorDefinition.RESERVATION_ALREADY_CANCELLED.toAppError();
        }
        if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
            throw ErrorDefinition.RESERVATION_ALREADY_CHECKED_IN.toAppError();
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        Reservation updatedReservation = reservationRepository.save(reservation);

        log.info("Reservation {} checked in successfully", id);
        return reservationMapper.toResponse(updatedReservation);
    }

    @Override
    public ReservationResponse checkOut(UUID id) {
        log.debug("Checking out reservation with ID: {}", id);

        Reservation reservation = findReservationById(id);

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            throw ErrorDefinition.RESERVATION_CANNOT_MODIFY.toAppError()
                    .withDetail("message", "Must be checked in before checking out");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        Reservation updatedReservation = reservationRepository.save(reservation);

        log.info("Reservation {} checked out successfully", id);
        return reservationMapper.toResponse(updatedReservation);
    }

    @Override
    public ReservationResponse cancelReservation(UUID id) {
        log.debug("Cancelling reservation with ID: {}", id);

        Reservation reservation = findReservationById(id);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw ErrorDefinition.RESERVATION_ALREADY_CANCELLED.toAppError();
        }
        if (reservation.getStatus() == ReservationStatus.CHECKED_IN ||
                reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            throw ErrorDefinition.RESERVATION_CANNOT_MODIFY.toAppError()
                    .withDetail("status", reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation updatedReservation = reservationRepository.save(reservation);

        log.info("Reservation {} cancelled successfully", id);
        return reservationMapper.toResponse(updatedReservation);
    }

    @Override
    public void deleteReservation(UUID id) {
        log.debug("Deleting reservation with ID: {}", id);

        Reservation reservation = findReservationById(id);

        // Only allow deletion of cancelled reservations
        if (reservation.getStatus() != ReservationStatus.CANCELLED) {
            throw ErrorDefinition.RESERVATION_CANNOT_MODIFY.toAppError()
                    .withDetail("message", "Only cancelled reservations can be deleted");
        }

        reservationRepository.delete(reservation);
        log.info("Reservation {} deleted successfully", id);
    }

    // ========== Private Helper Methods ==========

    private Reservation findReservationById(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> ErrorDefinition.RESERVATION_NOT_FOUND.toAppError()
                        .withDetail("reservationId", id));
    }

    private void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.isBefore(LocalDate.now())) {
            throw ErrorDefinition.PAST_CHECK_IN_DATE.toAppError()
                    .withDetail("checkIn", checkIn);
        }
        if (!checkOut.isAfter(checkIn)) {
            throw ErrorDefinition.INVALID_DATE_RANGE.toAppError()
                    .withDetail("checkIn", checkIn)
                    .withDetail("checkOut", checkOut);
        }
    }

    private List<Room> validateAndGetRooms(List<UUID> roomIds, LocalDate checkIn, LocalDate checkOut,
            UUID excludeReservationId) {
        List<Room> rooms = new ArrayList<>();

        for (UUID roomId : roomIds) {
            Room room = roomRepository.findById(roomId.toString())
                    .orElseThrow(() -> ErrorDefinition.ROOM_NOT_FOUND.toAppError()
                            .withDetail("roomId", roomId));

            // Check availability
            boolean hasConflict;
            if (excludeReservationId != null) {
                hasConflict = reservationRoomRepository.hasConflictingReservationExcluding(
                        room.getId(), checkIn, checkOut, excludeReservationId);
            } else {
                hasConflict = reservationRoomRepository.hasConflictingReservation(
                        room.getId(), checkIn, checkOut);
            }

            if (hasConflict) {
                throw ErrorDefinition.ROOM_NOT_AVAILABLE.toAppError()
                        .withDetail("roomId", roomId)
                        .withDetail("roomNumber", room.getRoomNumber());
            }

            rooms.add(room);
        }

        return rooms;
    }

    private BigDecimal calculateTotalAmount(List<Room> rooms, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights < 1)
            nights = 1;

        BigDecimal total = BigDecimal.ZERO;
        for (Room room : rooms) {
            if (room.getType() != null && room.getType().getPricePerNight() != null) {
                BigDecimal roomPrice = BigDecimal.valueOf(room.getType().getPricePerNight())
                        .multiply(BigDecimal.valueOf(nights));
                total = total.add(roomPrice);
            }
        }
        return total;
    }
}
