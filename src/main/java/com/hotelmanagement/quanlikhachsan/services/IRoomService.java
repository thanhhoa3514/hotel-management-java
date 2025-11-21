package com.hotelmanagement.quanlikhachsan.services;

import com.hotelmanagement.quanlikhachsan.dto.request.RoomRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomResponse;
import com.hotelmanagement.quanlikhachsan.model.room.RoomStatus;

import java.util.List;
import java.util.Optional;

public interface IRoomService {
    List<RoomResponse> getAllRooms();

    Optional<RoomResponse> getRoomById(String roomId);

    List<RoomResponse> getRoomsByStatus(RoomStatus status);

    List<RoomResponse> getRoomsByStatusName(String statusName);

    RoomResponse createRoom(RoomRequest request);

    RoomResponse updateRoom(String roomId, RoomRequest request);

    void deleteRoom(String roomId);
}
