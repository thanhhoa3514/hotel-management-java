package com.hotelmanagement.quanlikhachsan.dto.response.room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for room availability check.
 */
public record RoomAvailabilityResponse(
        boolean allAvailable,
        List<RoomAvailabilityDetail> rooms,
        LocalDate checkIn,
        LocalDate checkOut,
        long nights,
        BigDecimal estimatedTotal) {
    public record RoomAvailabilityDetail(
            String roomId,
            String roomNumber,
            boolean available,
            String roomType,
            BigDecimal pricePerNight) {
    }
}
