package com.hotelmanagement.quanlikhachsan.controller;

import com.hotelmanagement.quanlikhachsan.dto.response.ApiResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomStatusResponse;
import com.hotelmanagement.quanlikhachsan.services.IRoomStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Room Status management.
 */
@RestController
@RequestMapping("/api/v1/room-statuses")
@RequiredArgsConstructor
@Slf4j
public class RoomStatusController {

    private final IRoomStatusService roomStatusService;

    /**
     * Get all room statuses
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomStatusResponse>>> getAllRoomStatuses() {
        log.info("Fetching all room statuses");
        List<RoomStatusResponse> roomStatuses = roomStatusService.getAllRoomStatuses();
        return ResponseEntity.ok(ApiResponse.success(roomStatuses));
    }

    /**
     * Get room status by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomStatusResponse>> getRoomStatusById(@PathVariable String id) {
        log.info("Fetching room status with ID: {}", id);
        RoomStatusResponse roomStatus = roomStatusService.getRoomStatusById(id);
        return ResponseEntity.ok(ApiResponse.success(roomStatus));
    }

    /**
     * Get room status by name
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<ApiResponse<RoomStatusResponse>> getRoomStatusByName(@PathVariable String name) {
        log.info("Fetching room status with name: {}", name);
        RoomStatusResponse roomStatus = roomStatusService.getRoomStatusByName(name);
        return ResponseEntity.ok(ApiResponse.success(roomStatus));
    }
}
