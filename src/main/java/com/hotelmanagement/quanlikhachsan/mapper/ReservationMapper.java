package com.hotelmanagement.quanlikhachsan.mapper;


import com.hotelmanagement.quanlikhachsan.dto.response.RoomImageResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomTypeResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.guest.GuestResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.reservation.ReservationResponse;
import com.hotelmanagement.quanlikhachsan.model.reservation.Reservation;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ReservationMapper {

    // Reservation mappings
    public ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                new GuestResponse(reservation.getGuest().getId(), reservation.getGuest().getFullName(),
                        reservation.getGuest().getKeycloakUserId(), reservation.getGuest().getCreatedAt(),
                        reservation.getGuest().getUpdatedAt()),
                reservation.getReservationRooms().stream()
                        .map(room -> new RoomResponse(
                                room.getRoom().getId(), room.getRoom().getRoomNumber(),
                                new RoomTypeResponse(room.getRoom().getId(),
                                        room.getRoom().getName(),
                                        room.getRoom().getRoomType().getDescription(),
                                        room.getRoom().getRoomType().getPricePerNight()),
                                room.getRoom().getRoomStatus(), room.getRoom().getFloor(), room.getRoom().getNote(),
                                room.getRoom().getImages().stream()
                                        .map(image -> new RoomImageResponse(image.getId(), image.getRoom().getId(),
                                                image.getImageUrl(), image.getDescription(), image.getIsPrimary(),
                                                image.getDisplayOrder(), image.getCreatedAt()))
                                        .collect(Collectors.toList())))
                        .collect(Collectors.toList()),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getTotalAmount(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }
}
