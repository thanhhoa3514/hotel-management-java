package com.hotelmanagement.quanlikhachsan.services;

import com.hotelmanagement.quanlikhachsan.dto.request.RoomRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomResponse;
import com.hotelmanagement.quanlikhachsan.exception.ErrorDefinition;
import com.hotelmanagement.quanlikhachsan.mapper.RoomMapper;

import com.hotelmanagement.quanlikhachsan.model.room.Room;
import com.hotelmanagement.quanlikhachsan.model.room.RoomStatus;
import com.hotelmanagement.quanlikhachsan.model.room.RoomType;
import com.hotelmanagement.quanlikhachsan.repository.RoomRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements IRoomService{


    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;

    /*
    * Return all rooms in hotel
    *
    * */
    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<RoomResponse> getRoomById(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> ErrorDefinition.ROOM_NOT_FOUND.toAppError().withDetail("productId", roomId));
        return Optional.ofNullable(roomMapper.toResponse(room));
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByStatus(RoomStatus status) {
        return roomRepository.findAllByStatus(status).stream()
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByStatusName(String statusName) {
        return roomRepository.findAllByStatusName(statusName).stream()
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        if (roomRepository.existsByRoomNumber(request.roomNumber())) {
            throw ErrorDefinition.DUPLICATE_ID.toAppError().withDetail("roomNumber", request.roomNumber());
        }

        Room room = roomMapper.toEntity(request);
        Room savedRoom = roomRepository.save(room);

        return roomMapper.toResponse(savedRoom);
    }


    @Override
    @Transactional
    public RoomResponse updateRoom(String roomId, RoomRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> ErrorDefinition.ROOM_NOT_FOUND.toAppError().withDetail("roomId", roomId));

        if (!room.getRoomNumber().equals(request.roomNumber()) &&
                roomRepository.existsByRoomNumber(request.roomNumber())) {
            throw ErrorDefinition.DUPLICATE_ID.toAppError().withDetail("roomNumber", request.roomNumber());
        }

        room.setRoomNumber(request.roomNumber());
        room.setType(RoomType.builder().id(request.roomTypeId()).build());
        if (request.roomStatusId() != null) {
            room.setStatus(RoomStatus.builder().id(request.roomStatusId()).build());
        }
        room.setFloor(request.floor());
        room.setNote(request.note());

        Room updatedRoom = roomRepository.save(room);
        return roomMapper.toResponse(updatedRoom);
    }

    @Override
    @Transactional
    public void deleteRoom(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> ErrorDefinition.ROOM_NOT_FOUND.toAppError().withDetail("roomId", roomId));
        
        // Kiểm tra trạng thái phòng trước khi xóa
        String statusName = room.getStatus().getName();
        if ("Occupied".equalsIgnoreCase(statusName) || 
            "Reserved".equalsIgnoreCase(statusName) ||
            "Booked".equalsIgnoreCase(statusName)) {
            throw ErrorDefinition.ROOM_IN_USE.toAppError()
                    .withDetail("roomNumber", room.getRoomNumber())
                    .withDetail("status", statusName);
        }
        
        roomRepository.delete(room);
    }
}
