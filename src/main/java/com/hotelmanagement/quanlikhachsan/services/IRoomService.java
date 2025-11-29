package com.hotelmanagement.quanlikhachsan.services;

import com.hotelmanagement.quanlikhachsan.dto.request.room.RoomAvailabilityRequest;
import com.hotelmanagement.quanlikhachsan.dto.request.room.RoomRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.room.RoomAvailabilityResponse;
import com.hotelmanagement.quanlikhachsan.model.room.RoomStatus;

import java.time.LocalDate;
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

    /**
     * Check availability of rooms for a given date range.
     *
     * @param request containing room IDs and date range
     * @return availability status for each room with estimated total
     */
    RoomAvailabilityResponse checkAvailability(RoomAvailabilityRequest request);

    /**
     * Get all available rooms for a given date range.
     *
     * @param checkIn  check-in date
     * @param checkOut check-out date
     * @return list of available rooms
     */
    List<RoomResponse> getAvailableRooms(LocalDate checkIn, LocalDate checkOut);
}
