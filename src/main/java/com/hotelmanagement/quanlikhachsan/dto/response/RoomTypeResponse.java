package com.hotelmanagement.quanlikhachsan.dto.response;

import java.math.BigDecimal;

public record RoomTypeResponse(
                String id,
                String name,
                String description,
                BigDecimal pricePerNight) {
}
