package com.hotelmanagement.quanlikhachsan.dto.response;

import java.util.List;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        String roomNumber,
        RoomTypeResponse roomType,
        RoomStatusResponse roomStatus,
        short floor,
        String note,
        List<RoomImageResponse> images
) {
}
