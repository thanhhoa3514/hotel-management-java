package com.hotelmanagement.quanlikhachsan.services;

import com.hotelmanagement.quanlikhachsan.dto.response.RoomStatusResponse;

import java.util.List;

public interface IRoomStatusService {
    List<RoomStatusResponse> getAllRoomStatuses();

    RoomStatusResponse getRoomStatusById(String id);

    RoomStatusResponse getRoomStatusByName(String name);
}
