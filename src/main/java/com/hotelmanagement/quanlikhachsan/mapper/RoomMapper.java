package com.hotelmanagement.quanlikhachsan.mapper;

import com.hotelmanagement.quanlikhachsan.dto.request.room.RoomRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomStatusResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomTypeResponse;
import com.hotelmanagement.quanlikhachsan.model.room.Room;
import com.hotelmanagement.quanlikhachsan.model.room.RoomStatus;
import com.hotelmanagement.quanlikhachsan.model.room.RoomType;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
@Component
public class RoomMapper {

    private final RoomImageMapper roomImageMapper;

    public RoomMapper(RoomImageMapper roomImageMapper) {
        this.roomImageMapper = roomImageMapper;
    }

    public RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getRoomNumber(),
                new RoomTypeResponse(room.getType().getId(), room.getType().getName(),
                        room.getType().getDescription(), room.getType().getPricePerNight()),
                new RoomStatusResponse(room.getStatus().getId(), room.getStatus().getName()),
                room.getFloor(),
                room.getNote(),
                room.getImages().stream()
                        .map(roomImageMapper::toResponse)
                        .collect(Collectors.toList()));
    }

    public Room toEntity(RoomRequest request) {
        RoomType roomType = RoomType.builder().id(request.roomTypeId()).build();
        RoomStatus roomStatus = request.roomStatusId() != null 
            ? RoomStatus.builder().id(request.roomStatusId()).build()
            : null;

        return Room.builder()
                .roomNumber(request.roomNumber())
                .type(roomType)
                .status(roomStatus)
                .floor(request.floor())
                .note(request.note())
                .build();
    }

}
