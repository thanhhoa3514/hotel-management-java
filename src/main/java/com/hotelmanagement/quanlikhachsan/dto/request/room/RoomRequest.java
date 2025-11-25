package com.hotelmanagement.quanlikhachsan.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomRequest(
        @NotBlank(message = "Room number is required")
        String roomNumber,

        @NotNull(message = "Room type ID is required")
        String roomTypeId,

        String roomStatusId,
        short floor,
        String note
) {
}
