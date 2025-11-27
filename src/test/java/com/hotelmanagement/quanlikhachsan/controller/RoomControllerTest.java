package com.hotelmanagement.quanlikhachsan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelmanagement.quanlikhachsan.dto.request.room.RoomRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomStatusResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomTypeResponse;
import com.hotelmanagement.quanlikhachsan.services.IRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IRoomService roomService;

    @Autowired
    private ObjectMapper objectMapper;

    private RoomRequest roomRequest;
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
    void getAllRooms_Success() throws Exception {
        when(roomService.getAllRooms()).thenReturn(List.of(roomResponse));

        mockMvc.perform(get("/api/v1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(roomId));
    }

    @Test
    void getRoomById_Success() throws Exception {
        when(roomService.getRoomById(roomId)).thenReturn(Optional.of(roomResponse));

        mockMvc.perform(get("/api/v1/rooms/{id}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(roomId));
    }

    @Test
    void getRoomsByStatus_Success() throws Exception {
        when(roomService.getRoomsByStatusName("Available")).thenReturn(List.of(roomResponse));

        mockMvc.perform(get("/api/v1/rooms/status/{statusName}", "Available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(roomId));
    }

    @Test
    void createRoom_Success() throws Exception {
        when(roomService.createRoom(any(RoomRequest.class))).thenReturn(roomResponse);

        mockMvc.perform(post("/api/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(roomId));
    }

    @Test
    void updateRoom_Success() throws Exception {
        when(roomService.updateRoom(eq(roomId), any(RoomRequest.class))).thenReturn(roomResponse);

        mockMvc.perform(patch("/api/v1/rooms/{id}", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(roomId));
    }

    @Test
    void deleteRoom_Success() throws Exception {
        doNothing().when(roomService).deleteRoom(roomId);

        mockMvc.perform(delete("/api/v1/rooms/{id}", roomId))
                .andExpect(status().isOk());
    }
}
