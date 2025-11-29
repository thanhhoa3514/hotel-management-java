package com.hotelmanagement.quanlikhachsan.controller;

import com.hotelmanagement.quanlikhachsan.dto.request.reservation.ReservationRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.ApiResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.reservation.ReservationResponse;
import com.hotelmanagement.quanlikhachsan.services.reservation.IReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Reservation management.
 * Provides endpoints for creating, reading, updating, and managing
 * reservations.
 */
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Slf4j
public class ReservationController {

    private final IReservationService reservationService;

    /**
     * Create a new reservation.
     *
     * @param request the reservation request containing guest, rooms, and dates
     * @return the created reservation
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @Valid @RequestBody ReservationRequest request) {
        log.info("Creating reservation for keycloakUserId: {}", request.keycloakUserId());
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reservation created successfully", response));
    }

    /**
     * Get a reservation by its ID.
     *
     * @param id the reservation ID
     * @return the reservation details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservationById(@PathVariable UUID id) {
        log.info("Fetching reservation with ID: {}", id);
        ReservationResponse response = reservationService.getReservationById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all reservations.
     *
     * @return list of all reservations
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getAllReservations() {
        log.info("Fetching all reservations");
        List<ReservationResponse> response = reservationService.getAllReservations();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get reservations by guest's Keycloak user ID.
     *
     * @param keycloakUserId the Keycloak user ID of the guest
     * @return list of reservations for the guest
     */
    @GetMapping("/guest/{keycloakUserId}")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getReservationsByGuestId(
            @PathVariable UUID keycloakUserId) {
        log.info("Fetching reservations for guest with keycloakUserId: {}", keycloakUserId);
        List<ReservationResponse> response = reservationService.getReservationsByGuestId(keycloakUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get reservations within a date range.
     *
     * @param startDate the start date of the range
     * @param endDate   the end date of the range
     * @return list of reservations within the date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getReservationsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Fetching reservations between {} and {}", startDate, endDate);
        List<ReservationResponse> response = reservationService.getReservationsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update an existing reservation.
     *
     * @param id      the reservation ID
     * @param request the updated reservation data
     * @return the updated reservation
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateReservation(
            @PathVariable UUID id,
            @Valid @RequestBody ReservationRequest request) {
        log.info("Updating reservation with ID: {}", id);
        ReservationResponse response = reservationService.updateReservation(id, request);
        return ResponseEntity.ok(ApiResponse.success("Reservation updated successfully", response));
    }

    /**
     * Add a room to an existing reservation.
     *
     * @param id     the reservation ID
     * @param roomId the room ID to add
     * @return the updated reservation
     */
    @PostMapping("/{id}/rooms/{roomId}")
    public ResponseEntity<ApiResponse<ReservationResponse>> addRoomToReservation(
            @PathVariable UUID id,
            @PathVariable UUID roomId) {
        log.info("Adding room {} to reservation {}", roomId, id);
        ReservationResponse response = reservationService.addRoomToReservation(id, roomId);
        return ResponseEntity.ok(ApiResponse.success("Room added to reservation", response));
    }

    /**
     * Remove a room from an existing reservation.
     *
     * @param id     the reservation ID
     * @param roomId the room ID to remove
     * @return success response
     */
    @DeleteMapping("/{id}/rooms/{roomId}")
    public ResponseEntity<ApiResponse<Void>> removeRoomFromReservation(
            @PathVariable UUID id,
            @PathVariable UUID roomId) {
        log.info("Removing room {} from reservation {}", roomId, id);
        reservationService.removeRoomFromReservation(id, roomId);
        return ResponseEntity.ok(ApiResponse.success("Room removed from reservation", null));
    }

    /**
     * Check-in a reservation.
     *
     * @param id the reservation ID
     * @return the updated reservation with CHECKED_IN status
     */
    @PostMapping("/{id}/check-in")
    public ResponseEntity<ApiResponse<ReservationResponse>> checkIn(@PathVariable UUID id) {
        log.info("Checking in reservation with ID: {}", id);
        ReservationResponse response = reservationService.checkIn(id);
        return ResponseEntity.ok(ApiResponse.success("Check-in successful", response));
    }

    /**
     * Check-out a reservation.
     *
     * @param id the reservation ID
     * @return the updated reservation with CHECKED_OUT status
     */
    @PostMapping("/{id}/check-out")
    public ResponseEntity<ApiResponse<ReservationResponse>> checkOut(@PathVariable UUID id) {
        log.info("Checking out reservation with ID: {}", id);
        ReservationResponse response = reservationService.checkOut(id);
        return ResponseEntity.ok(ApiResponse.success("Check-out successful", response));
    }

    /**
     * Cancel a reservation.
     *
     * @param id the reservation ID
     * @return the updated reservation with CANCELLED status
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ReservationResponse>> cancelReservation(@PathVariable UUID id) {
        log.info("Cancelling reservation with ID: {}", id);
        ReservationResponse response = reservationService.cancelReservation(id);
        return ResponseEntity.ok(ApiResponse.success("Reservation cancelled successfully", response));
    }

    /**
     * Delete a reservation (only cancelled reservations can be deleted).
     *
     * @param id the reservation ID
     * @return success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReservation(@PathVariable UUID id) {
        log.info("Deleting reservation with ID: {}", id);
        reservationService.deleteReservation(id);
        return ResponseEntity.ok(ApiResponse.success("Reservation deleted successfully", null));
    }
}
