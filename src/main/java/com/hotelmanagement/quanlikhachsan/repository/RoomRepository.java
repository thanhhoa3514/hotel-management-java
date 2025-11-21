package com.hotelmanagement.quanlikhachsan.repository;

import com.hotelmanagement.quanlikhachsan.model.room.Room;
import com.hotelmanagement.quanlikhachsan.model.room.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    Optional<Room> findById(String roomId);

    Optional<Room> findByRoomNumber(String roomNumber);

    List<Room> findAllByStatus(RoomStatus roomStatus);

    List<Room> findAllByStatusName(String statusName);

    boolean existsByRoomNumber(String roomNumber);
}
