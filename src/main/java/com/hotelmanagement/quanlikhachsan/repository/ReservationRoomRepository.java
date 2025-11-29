package com.hotelmanagement.quanlikhachsan.repository;

import com.hotelmanagement.quanlikhachsan.model.reservation.ReservationRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRoomRepository extends JpaRepository<ReservationRoom, UUID> {

    List<ReservationRoom> findByReservationId(UUID reservationId);

    List<ReservationRoom> findByRoomId(String roomId);

    void deleteByReservationIdAndRoomId(UUID reservationId, String roomId);

    boolean existsByReservationIdAndRoomId(UUID reservationId, String roomId);

    /**
     * Check if a room is available for a given date range.
     * A room is not available if there's an overlapping reservation that is not
     * cancelled or checked out.
     */
    @Query("""
            SELECT COUNT(rr) > 0 FROM ReservationRoom rr
            JOIN rr.reservation r
            WHERE rr.room.id = :roomId
            AND r.status NOT IN (com.hotelmanagement.quanlikhachsan.model.reservation.ReservationStatus.CANCELLED,
                                  com.hotelmanagement.quanlikhachsan.model.reservation.ReservationStatus.CHECKED_OUT)
            AND ((r.checkIn <= :checkOut AND r.checkOut >= :checkIn))
            """)
    boolean hasConflictingReservation(
            @Param("roomId") String roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut);

    /**
     * Check if a room is available for a given date range, excluding a specific
     * reservation.
     * Used for updating reservations.
     */
    @Query("""
            SELECT COUNT(rr) > 0 FROM ReservationRoom rr
            JOIN rr.reservation r
            WHERE rr.room.id = :roomId
            AND r.id != :excludeReservationId
            AND r.status NOT IN (com.hotelmanagement.quanlikhachsan.model.reservation.ReservationStatus.CANCELLED,
                                  com.hotelmanagement.quanlikhachsan.model.reservation.ReservationStatus.CHECKED_OUT)
            AND ((r.checkIn <= :checkOut AND r.checkOut >= :checkIn))
            """)
    boolean hasConflictingReservationExcluding(
            @Param("roomId") String roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludeReservationId") UUID excludeReservationId);
}
