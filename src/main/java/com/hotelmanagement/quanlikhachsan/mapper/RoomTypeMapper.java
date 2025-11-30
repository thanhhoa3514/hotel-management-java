package com.hotelmanagement.quanlikhachsan.mapper;

import com.hotelmanagement.quanlikhachsan.dto.response.RoomTypeResponse;
import com.hotelmanagement.quanlikhachsan.model.room.RoomType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class   RoomTypeMapper {

    public RoomTypeResponse toResponse(RoomType roomType) {
        if (roomType == null) {
            return null;
        }

        return new RoomTypeResponse(
                UUID.fromString(roomType.getId()),
                roomType.getName(),
                roomType.getDescription(),
                roomType.getPricePerNight(),
                roomType.getCapacity(),
                roomType.getSize());
    }
}
