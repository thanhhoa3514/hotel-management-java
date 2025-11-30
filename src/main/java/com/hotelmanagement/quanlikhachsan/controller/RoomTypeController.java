package com.hotelmanagement.quanlikhachsan.controller;

import com.hotelmanagement.quanlikhachsan.dto.response.ApiResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomTypeResponse;
import com.hotelmanagement.quanlikhachsan.services.IRoomTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Room Type management.
 */
@RestController
@RequestMapping("/api/v1/room-types")
@RequiredArgsConstructor
@Slf4j
public class RoomTypeController {

    private final IRoomTypeService roomTypeService;

    /**
     * Get all room types
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomTypeResponse>>> getAllRoomTypes() {
        log.info("Fetching all room types");
        List<RoomTypeResponse> roomTypes = roomTypeService.getAllRoomTypes();
        return ResponseEntity.ok(ApiResponse.success(roomTypes));
    }

    /**
     * Get room type by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> getRoomTypeById(@PathVariable String id) {
        log.info("Fetching room type with ID: {}", id);
        RoomTypeResponse roomType = roomTypeService.getRoomTypeById(id);
        return ResponseEntity.ok(ApiResponse.success(roomType));
    }

    /**
     * Get room type by name
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> getRoomTypeByName(@PathVariable String name) {
        log.info("Fetching room type with name: {}", name);
        RoomTypeResponse roomType = roomTypeService.getRoomTypeByName(name);
        return ResponseEntity.ok(ApiResponse.success(roomType));
    }
}
