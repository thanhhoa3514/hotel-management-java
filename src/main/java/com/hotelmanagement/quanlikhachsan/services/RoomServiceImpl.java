package com.hotelmanagement.quanlikhachsan.services;

import com.hotelmanagement.quanlikhachsan.dto.request.room.RoomAvailabilityRequest;
import com.hotelmanagement.quanlikhachsan.dto.request.room.RoomRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.room.RoomAvailabilityResponse;
import com.hotelmanagement.quanlikhachsan.exception.ErrorDefinition;
import com.hotelmanagement.quanlikhachsan.mapper.RoomMapper;

import com.hotelmanagement.quanlikhachsan.model.room.Room;
import com.hotelmanagement.quanlikhachsan.model.room.RoomStatus;
import com.hotelmanagement.quanlikhachsan.model.room.RoomType;
import com.hotelmanagement.quanlikhachsan.repository.ReservationRoomRepository;
import com.hotelmanagement.quanlikhachsan.repository.RoomRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements IRoomService {

    private final RoomRepository roomRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final RoomMapper roomMapper;

    /*
     * Return all rooms in hotel
     *
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<RoomResponse> getRoomById(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> ErrorDefinition.ROOM_NOT_FOUND.toAppError().withDetail("productId", roomId));
        return Optional.ofNullable(roomMapper.toResponse(room));
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByStatus(RoomStatus status) {
        return roomRepository.findAllByStatus(status).stream()
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByStatusName(String statusName) {
        return roomRepository.findAllByStatusName(statusName).stream()
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        if (roomRepository.existsByRoomNumber(request.roomNumber())) {
            throw ErrorDefinition.DUPLICATE_ID.toAppError().withDetail("roomNumber", request.roomNumber());
        }

        Room room = roomMapper.toEntity(request);
        Room savedRoom = roomRepository.save(room);

        return roomMapper.toResponse(savedRoom);
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(String roomId, RoomRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> ErrorDefinition.ROOM_NOT_FOUND.toAppError().withDetail("roomId", roomId));

        if (!room.getRoomNumber().equals(request.roomNumber()) &&
                roomRepository.existsByRoomNumber(request.roomNumber())) {
            throw ErrorDefinition.DUPLICATE_ID.toAppError().withDetail("roomNumber", request.roomNumber());
        }

        room.setRoomNumber(request.roomNumber());
        room.setType(RoomType.builder().id(request.roomTypeId()).build());
        if (request.roomStatusId() != null) {
            room.setStatus(RoomStatus.builder().id(request.roomStatusId()).build());
        }
        room.setFloor(request.floor());
        room.setNote(request.note());

        Room updatedRoom = roomRepository.save(room);
        return roomMapper.toResponse(updatedRoom);
    }

    @Override
    @Transactional
    public void deleteRoom(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> ErrorDefinition.ROOM_NOT_FOUND.toAppError().withDetail("roomId", roomId));

        // Kiểm tra trạng thái phòng trước khi xóa
        String statusName = room.getStatus().getName();
        if ("Occupied".equalsIgnoreCase(statusName) ||
                "Reserved".equalsIgnoreCase(statusName) ||
                "Booked".equalsIgnoreCase(statusName)) {
            throw ErrorDefinition.ROOM_IN_USE.toAppError()
                    .withDetail("roomNumber", room.getRoomNumber())
                    .withDetail("status", statusName);
        }

        roomRepository.delete(room);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomAvailabilityResponse checkAvailability(RoomAvailabilityRequest request) {
        List<RoomAvailabilityResponse.RoomAvailabilityDetail> details = new ArrayList<>();
        boolean allAvailable = true;
        BigDecimal estimatedTotal = BigDecimal.ZERO;
        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights < 1)
            nights = 1;

        List<String> roomIds = request.roomIds();
        if (roomIds == null || roomIds.isEmpty()) {
            // If no specific rooms provided, check all rooms
            roomIds = roomRepository.findAll().stream()
                    .map(Room::getId)
                    .collect(Collectors.toList());
        }

        for (String roomId : roomIds) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> ErrorDefinition.ROOM_NOT_FOUND.toAppError()
                            .withDetail("roomId", roomId));

            boolean hasConflict = reservationRoomRepository.hasConflictingReservation(
                    room.getId(), request.checkIn(), request.checkOut());

            boolean isAvailable = !hasConflict;
            if (!isAvailable) {
                allAvailable = false;
            }

            BigDecimal pricePerNight = room.getType() != null && room.getType().getPricePerNight() != null
                    ? room.getType().getPricePerNight()
                    : BigDecimal.ZERO;

            if (isAvailable) {
                estimatedTotal = estimatedTotal.add(pricePerNight.multiply(BigDecimal.valueOf(nights)));
            }

            details.add(new RoomAvailabilityResponse.RoomAvailabilityDetail(
                    room.getId(),
                    room.getRoomNumber(),
                    isAvailable,
                    room.getType() != null ? room.getType().getName() : null,
                    pricePerNight));
        }

        return new RoomAvailabilityResponse(
                allAvailable,
                details,
                request.checkIn(),
                request.checkOut(),
                nights,
                estimatedTotal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        return roomRepository.findAll().stream()
                .filter(room -> !reservationRoomRepository.hasConflictingReservation(
                        room.getId(), checkIn, checkOut))
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());
    }
}
