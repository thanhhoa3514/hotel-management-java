package com.hotelmanagement.quanlikhachsan.services;

import com.hotelmanagement.quanlikhachsan.dto.response.RoomTypeResponse;

import java.util.List;

public interface IRoomTypeService {
    List<RoomTypeResponse> getAllRoomTypes();
    RoomTypeResponse getRoomTypeById(String id);
    RoomTypeResponse getRoomTypeByName(String name);
}

