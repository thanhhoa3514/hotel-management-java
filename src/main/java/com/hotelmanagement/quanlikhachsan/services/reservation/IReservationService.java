package com.hotelmanagement.quanlikhachsan.services.reservation;

import com.hotelmanagement.quanlikhachsan.dto.request.reservation.ReservationRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.reservation.ReservationResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for Reservation management
 */
public interface IReservationService {
    ReservationResponse createReservation(ReservationRequest request);

    ReservationResponse getReservationById(UUID id);

    List<ReservationResponse> getAllReservations();

    List<ReservationResponse> getReservationsByGuestId(UUID guestId);

    List<ReservationResponse> getReservationsByStatus(UUID statusId);

    List<ReservationResponse> getReservationsByDateRange(LocalDate startDate, LocalDate endDate);

    ReservationResponse updateReservation(UUID id, ReservationRequest request);

    ReservationResponse updateReservationStatus(UUID id, UUID statusId);

    ReservationResponse addRoomToReservation(UUID reservationId, UUID roomId);

    void removeRoomFromReservation(UUID reservationId, UUID roomId);

    ReservationResponse addServiceToReservation(UUID reservationId, UUID serviceId, Integer quantity);

    void removeServiceFromReservation(UUID reservationId, UUID serviceId);

    ReservationResponse checkIn(UUID id);

    ReservationResponse checkOut(UUID id);

    ReservationResponse cancelReservation(UUID id);

    void deleteReservation(UUID id);

    /**
     * Count active reservations for a specific room.
     * Active means status is PENDING, CONFIRMED, or CHECKED_IN.
     *
     * @param roomId the room ID
     * @return count of active reservations
     */
    long countActiveReservationsByRoomId(String roomId);
}
