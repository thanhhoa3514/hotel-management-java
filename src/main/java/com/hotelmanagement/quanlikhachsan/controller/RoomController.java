package com.hotelmanagement.quanlikhachsan.controller;

import com.hotelmanagement.quanlikhachsan.dto.request.room.RoomAvailabilityRequest;
import com.hotelmanagement.quanlikhachsan.dto.request.room.RoomRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.ApiResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.room.RoomAvailabilityResponse;
import com.hotelmanagement.quanlikhachsan.services.IRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {
    private final IRoomService roomService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAllRooms() {
        List<RoomResponse> response = roomService.getAllRooms();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Optional<RoomResponse>>> getRoomById(@PathVariable String id) {
        Optional<RoomResponse> response = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status/{statusName}")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByStatus(@PathVariable String statusName) {
        List<RoomResponse> response = roomService.getRoomsByStatusName(statusName);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Create a new room with images
     * Accepts multipart/form-data with room info and image files
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @RequestPart("room") @Valid RoomRequest request,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            @RequestPart(value = "imageOrder", required = false) List<String> imageOrder) {
        log.info("Creating room: {} with {} images", request.roomNumber(),
                images != null ? images.length : 0);
        RoomResponse response = roomService.createRoom(request, images, imageOrder);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room created successfully", response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable String id,
            @RequestPart("room") @Valid RoomRequest request,
            @RequestPart(value = "newImages", required = false) MultipartFile[] newImages,
            @RequestPart(value = "newImageOrder", required = false) List<String> newImageOrder) {
        log.info("Updating room: {} with {} new images", id,
                newImages != null ? newImages.length : 0);
        RoomResponse response = roomService.updateRoom(id, request, newImages, newImageOrder);
        return ResponseEntity.ok(ApiResponse.success("Room updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable String id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success("Room deleted successfully", null));
    }

    /**
     * Check availability of specific rooms for a date range.
     */
    @PostMapping("/check-availability")
    public ResponseEntity<ApiResponse<RoomAvailabilityResponse>> checkAvailability(
            @Valid @RequestBody RoomAvailabilityRequest request) {
        RoomAvailabilityResponse response = roomService.checkAvailability(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all available rooms for a date range.
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAvailableRooms(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        List<RoomResponse> response = roomService.getAvailableRooms(checkIn, checkOut);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Upload images for a room.
     * Accepts multiple image files and saves them to the server.
     *
     * @param roomId the room ID
     * @param files  the image files to upload
     * @return success response with uploaded image URLs
     */
    @PostMapping(value = "/{roomId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<String>>> uploadRoomImages(
            @PathVariable String roomId,
            @RequestParam("files") List<MultipartFile> files) {
        log.info("Uploading {} images for room ID: {}", files.size(), roomId);

        // Validate files
        if (files == null || files.isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("No files provided"));
        }

        // Validate file types and sizes
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(ApiResponse.error("Empty file detected"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity
                        .badRequest()
                        .body(ApiResponse.error("Only image files are allowed"));
            }

            // Max 5MB per file
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity
                        .badRequest()
                        .body(ApiResponse.error("File size must not exceed 5MB"));
            }
        }

        List<String> imageUrls = roomService.uploadRoomImages(roomId, files);
        return ResponseEntity.ok(ApiResponse.success("Images uploaded successfully", imageUrls));
    }

    /**
     * Delete a room image.
     *
     * @param roomId  the room ID
     * @param imageId the image ID to delete
     * @return success response
     */
    @DeleteMapping("/{roomId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoomImage(
            @PathVariable String roomId,
            @PathVariable String imageId) {
        log.info("Deleting image {} for room ID: {}", imageId, roomId);
        roomService.deleteRoomImage(roomId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully", null));
    }

    /**
     * Set primary image for a room.
     *
     * @param roomId  the room ID
     * @param imageId the image ID to set as primary
     * @return success response
     */
    @PatchMapping("/{roomId}/images/{imageId}/primary")
    public ResponseEntity<ApiResponse<Void>> setPrimaryImage(
            @PathVariable String roomId,
            @PathVariable String imageId) {
        log.info("Setting image {} as primary for room ID: {}", imageId, roomId);
        roomService.setPrimaryImage(roomId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Primary image updated successfully", null));
    }
}
