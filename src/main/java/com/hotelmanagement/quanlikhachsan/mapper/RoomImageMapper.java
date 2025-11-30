package com.hotelmanagement.quanlikhachsan.mapper;

import com.hotelmanagement.quanlikhachsan.dto.response.RoomImageResponse;
import com.hotelmanagement.quanlikhachsan.model.room.RoomImage;
import org.springframework.stereotype.Component;

@Component
public class RoomImageMapper {

    public RoomImageResponse toResponse(RoomImage image) {
        return new RoomImageResponse(
                image.getId(),
                image.getRoom().getId(),
                image.getImageUrl(),
                image.getDescription(),
                image.getIsPrimary(),
                image.getDisplayOrder(),
                image.getCreatedAt());
    }
}
