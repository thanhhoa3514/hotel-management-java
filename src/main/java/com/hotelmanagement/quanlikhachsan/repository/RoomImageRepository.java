package com.hotelmanagement.quanlikhachsan.repository;

import com.hotelmanagement.quanlikhachsan.model.room.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, String> {

    /**
     * Find all images for a specific room.
     *
     * @param roomId the room ID
     * @return list of room images
     */
    List<RoomImage> findByRoomId(String roomId);

    /**
     * Find primary image for a specific room.
     *
     * @param roomId    the room ID
     * @param isPrimary true to find primary image
     * @return list of primary images (should be only one)
     */
    List<RoomImage> findByRoomIdAndIsPrimary(String roomId, Boolean isPrimary);

    /**
     * Delete all images for a specific room.
     *
     * @param roomId the room ID
     */
    void deleteByRoomId(String roomId);
}
