package com.hotelmanagement.quanlikhachsan.dto.response.reservation;

import com.hotelmanagement.quanlikhachsan.dto.response.RoomResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.guest.GuestResponse;
import com.hotelmanagement.quanlikhachsan.model.reservation.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReservationResponse(
        Long id,
        GuestResponse guest,
        List<RoomResponse> rooms,
        LocalDate checkIn,
        LocalDate checkOut,
        BigDecimal totalAmount,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
