package com.hotelmanagement.quanlikhachsan.dto.request.room;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for checking room availability.
 */
public record RoomAvailabilityRequest(
        List<String> roomIds,

        @NotNull(message = "Check-in date is required") @FutureOrPresent(message = "Check-in date must be today or in the future") LocalDate checkIn,

        @NotNull(message = "Check-out date is required") @Future(message = "Check-out date must be in the future") LocalDate checkOut) {
}
