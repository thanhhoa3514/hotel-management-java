package com.hotelmanagement.quanlikhachsan.services;

import com.hotelmanagement.quanlikhachsan.dto.request.room.RoomRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomStatusResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomTypeResponse;
import com.hotelmanagement.quanlikhachsan.exception.AppError;
import com.hotelmanagement.quanlikhachsan.mapper.RoomMapper;
import com.hotelmanagement.quanlikhachsan.model.room.Room;
import com.hotelmanagement.quanlikhachsan.model.room.RoomStatus;
import com.hotelmanagement.quanlikhachsan.model.room.RoomType;
import com.hotelmanagement.quanlikhachsan.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomServiceImpl roomService;

    private RoomRequest roomRequest;
    private Room room;
    private RoomResponse roomResponse;
    private String roomId;

    @BeforeEach
    void setUp() {
        roomId = "room-123";
        roomRequest = new RoomRequest(
                "101",
                "type-1",
                "status-1",
                (short) 1,
                "Test Room"
        );

        room = new Room();
        room.setId(roomId);
        room.setRoomNumber("101");
        RoomStatus status = new RoomStatus();
        status.setName("Available");
        room.setStatus(status);

        roomResponse = new RoomResponse(
                roomId,
                "101",
                new RoomTypeResponse("type-1", "Standard", "Desc", 100.0),
                new RoomStatusResponse("status-1", "Available"),
                (short) 1,
                "Test Room",
                List.of()
        );
    }

    @Test
    void getAllRooms_Success() {
        when(roomRepository.findAll()).thenReturn(List.of(room));
        when(roomMapper.toResponse(room)).thenReturn(roomResponse);

        List<RoomResponse> responses = roomService.getAllRooms();

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals(roomId, responses.get(0).id());
    }

    @Test
    void getRoomById_Success() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMapper.toResponse(room)).thenReturn(roomResponse);

        Optional<RoomResponse> response = roomService.getRoomById(roomId);

        assertTrue(response.isPresent());
        assertEquals(roomId, response.get().id());
    }

    @Test
    void getRoomById_NotFound() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThrows(AppError.class, () -> roomService.getRoomById(roomId));
    }

    @Test
    void getRoomsByStatusName_Success() {
        when(roomRepository.findAllByStatusName("Available")).thenReturn(List.of(room));
        when(roomMapper.toResponse(room)).thenReturn(roomResponse);

        List<RoomResponse> responses = roomService.getRoomsByStatusName("Available");

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
    }

    @Test
    void createRoom_Success() {
        when(roomRepository.existsByRoomNumber(roomRequest.roomNumber())).thenReturn(false);
        when(roomMapper.toEntity(any(RoomRequest.class))).thenReturn(room);
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(roomMapper.toResponse(any(Room.class))).thenReturn(roomResponse);

        RoomResponse response = roomService.createRoom(roomRequest);

        assertNotNull(response);
        assertEquals(roomId, response.id());
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void createRoom_DuplicateNumber() {
        when(roomRepository.existsByRoomNumber(roomRequest.roomNumber())).thenReturn(true);

        assertThrows(AppError.class, () -> roomService.createRoom(roomRequest));
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void updateRoom_Success() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(roomMapper.toResponse(any(Room.class))).thenReturn(roomResponse);

        RoomResponse response = roomService.updateRoom(roomId, roomRequest);

        assertNotNull(response);
        assertEquals(roomId, response.id());
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void updateRoom_DuplicateNumber() {
        Room existingRoom = new Room();
        existingRoom.setId(roomId);
        existingRoom.setRoomNumber("102"); // Different number

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(existingRoom));
        when(roomRepository.existsByRoomNumber(roomRequest.roomNumber())).thenReturn(true);

        assertThrows(AppError.class, () -> roomService.updateRoom(roomId, roomRequest));
    }

    @Test
    void deleteRoom_Success() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        roomService.deleteRoom(roomId);

        verify(roomRepository).delete(room);
    }

    @Test
    void deleteRoom_InUse() {
        Room occupiedRoom = new Room();
        occupiedRoom.setId(roomId);
        occupiedRoom.setRoomNumber("101");
        RoomStatus status = new RoomStatus();
        status.setName("Occupied");
        occupiedRoom.setStatus(status);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(occupiedRoom));

        assertThrows(AppError.class, () -> roomService.deleteRoom(roomId));
        verify(roomRepository, never()).delete(any(Room.class));
    }
}
