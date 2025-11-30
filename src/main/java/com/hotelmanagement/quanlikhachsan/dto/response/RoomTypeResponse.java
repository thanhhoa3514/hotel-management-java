package com.hotelmanagement.quanlikhachsan.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomTypeResponse(
        UUID id,
        String name,
        String description,
        BigDecimal pricePerNight,
        Integer capacity,
        BigDecimal size) {
}
