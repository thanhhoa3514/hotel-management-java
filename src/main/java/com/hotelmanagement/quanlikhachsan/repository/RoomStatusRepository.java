package com.hotelmanagement.quanlikhachsan.repository;

import com.hotelmanagement.quanlikhachsan.model.room.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomStatusRepository extends JpaRepository<RoomStatus, String> {
    Optional<RoomStatus> findByName(String name);

    /**
     * Find room status by name (case-insensitive)
     */
    @Query("SELECT rs FROM RoomStatus rs WHERE LOWER(rs.name) = LOWER(:name)")
    Optional<RoomStatus> findByNameIgnoreCase(@Param("name") String name);
}
