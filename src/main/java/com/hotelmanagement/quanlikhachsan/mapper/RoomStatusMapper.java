package com.hotelmanagement.quanlikhachsan.mapper;

import com.hotelmanagement.quanlikhachsan.dto.response.RoomStatusResponse;
import com.hotelmanagement.quanlikhachsan.model.room.RoomStatus;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class RoomStatusMapper {

    public RoomStatusResponse toResponse(RoomStatus roomStatus) {
        if (roomStatus == null) {
            return null;
        }

        return new RoomStatusResponse(
                UUID.fromString(roomStatus.getId()),
                roomStatus.getName());
    }
}
