package com.hotelmanagement.quanlikhachsan.services.reservation;


import com.hotelmanagement.quanlikhachsan.dto.request.reservation.ReservationRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.reservation.ReservationResponse;
import com.hotelmanagement.quanlikhachsan.mapper.ReservationMapper;

import com.hotelmanagement.quanlikhachsan.model.guest.Guest;
import com.hotelmanagement.quanlikhachsan.repository.ReservationRepository;
import com.hotelmanagement.quanlikhachsan.repository.RoomRepository;
import com.hotelmanagement.quanlikhachsan.services.keycloak.IKeycloakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReservationService implements IReservationService {

    private final ReservationRepository reservationRepository;

    private final ReservationMapper reservationMapper;

   private final IKeycloakService keycloakService;

    private final RoomRepository roomRepository;


    @Override
    /*
     *
     *
     *
     *
     */

    public ReservationResponse createReservation(ReservationRequest request) {

        log.debug("Creating reservation for keycloak user ID: {}", request.keycloakUserId());

        Guest guest = keycloakService.findKeycloakByUserId(request.keycloakUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Guest", "keycloakUserId", request.keycloakUserId()));

        if(checkIsAfter(request.checkIn(),request.checkOut())){

            throw new BusinessException("Check-in date must be before check-out date");
        }
        return null;
    }

    @Override
    public ReservationResponse getReservationById(UUID id) {
        return null;
    }

    @Override
    public List<ReservationResponse> getAllReservations() {
        return List.of();
    }

    @Override
    public List<ReservationResponse> getReservationsByGuestId(UUID guestId) {
        return List.of();
    }

    @Override
    public List<ReservationResponse> getReservationsByStatus(UUID statusId) {
        return List.of();
    }

    @Override
    public List<ReservationResponse> getReservationsByDateRange(LocalDate startDate, LocalDate endDate) {
        return List.of();
    }

    @Override
    public ReservationResponse updateReservation(UUID id, ReservationRequest request) {
        return null;
    }

    @Override
    public ReservationResponse updateReservationStatus(UUID id, UUID statusId) {
        return null;
    }

    @Override
    public ReservationResponse addRoomToReservation(UUID reservationId, UUID roomId) {
        return null;
    }

    @Override
    public void removeRoomFromReservation(UUID reservationId, UUID roomId) {

    }

    @Override
    public ReservationResponse addServiceToReservation(UUID reservationId, UUID serviceId, Integer quantity) {
        return null;
    }

    @Override
    public void removeServiceFromReservation(UUID reservationId, UUID serviceId) {

    }

    @Override
    public ReservationResponse checkIn(UUID id) {
        return null;
    }

    @Override
    public ReservationResponse checkOut(UUID id) {
        return null;
    }

    @Override
    public ReservationResponse cancelReservation(UUID id) {
        return null;
    }

    @Override
    public void deleteReservation(UUID id) {

    }
    private boolean checkIfReserved(UUID id) {
        return false;
    }

    private boolean checkIsAfter(LocalDate date, LocalDate date2) {
        return date.isAfter(date2);
    }
}
