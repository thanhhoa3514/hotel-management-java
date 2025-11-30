package com.hotelmanagement.quanlikhachsan.services;

import com.hotelmanagement.quanlikhachsan.dto.request.room.RoomAvailabilityRequest;
import com.hotelmanagement.quanlikhachsan.dto.request.room.RoomRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.room.RoomAvailabilityResponse;
import com.hotelmanagement.quanlikhachsan.model.room.RoomStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IRoomService {
    List<RoomResponse> getAllRooms();

    Optional<RoomResponse> getRoomById(String roomId);

    List<RoomResponse> getRoomsByStatus(RoomStatus status);

    List<RoomResponse> getRoomsByStatusName(String statusName);

    RoomResponse createRoom(RoomRequest request, MultipartFile[] images, List<String> imageOrder);

    RoomResponse updateRoom(String roomId, RoomRequest request, MultipartFile[] newImages,
            List<String> newImageOrder);

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

    /**
     * Upload images for a room.
     *
     * @param roomId the room ID
     * @param files  the image files to upload
     * @return list of uploaded image URLs
     */
    List<String> uploadRoomImages(String roomId, List<MultipartFile> files);

    /**
     * Delete a room image.
     *
     * @param roomId  the room ID
     * @param imageId the image ID to delete
     */
    void deleteRoomImage(String roomId, String imageId);

    /**
     * Set an image as primary for a room.
     *
     * @param roomId  the room ID
     * @param imageId the image ID to set as primary
     */
    void setPrimaryImage(String roomId, String imageId);
}
