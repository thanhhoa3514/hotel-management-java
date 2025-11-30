package com.hotelmanagement.quanlikhachsan.services;

import com.hotelmanagement.quanlikhachsan.dto.request.room.RoomAvailabilityRequest;
import com.hotelmanagement.quanlikhachsan.dto.request.room.RoomRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.RoomResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.room.RoomAvailabilityResponse;
import com.hotelmanagement.quanlikhachsan.exception.ErrorDefinition;
import com.hotelmanagement.quanlikhachsan.mapper.RoomMapper;

import com.hotelmanagement.quanlikhachsan.model.room.Room;
import com.hotelmanagement.quanlikhachsan.model.room.RoomImage;
import com.hotelmanagement.quanlikhachsan.model.room.RoomStatus;
import com.hotelmanagement.quanlikhachsan.model.room.RoomType;
import com.hotelmanagement.quanlikhachsan.repository.ReservationRoomRepository;
import com.hotelmanagement.quanlikhachsan.repository.RoomImageRepository;
import com.hotelmanagement.quanlikhachsan.repository.RoomRepository;
import com.hotelmanagement.quanlikhachsan.repository.RoomStatusRepository;
import com.hotelmanagement.quanlikhachsan.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RoomServiceImpl implements IRoomService {

    private final RoomRepository roomRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final RoomMapper roomMapper;
    private final FileStorageService fileStorageService;
    private final RoomImageRepository roomImageRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomStatusRepository roomStatusRepository;

    /*
     * Return all rooms in hotel
     *
     */
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

    /**
     * ✅ FLOW CHUẨN: Create room WITH images
     * Bước 1: Validate room number không trùng
     * Bước 2: Validate và set default status nếu null
     * Bước 3: Lưu Room entity trước để có room.id
     * Bước 4: Xử lý vòng lặp ảnh - lưu từng file và tạo RoomImage
     */
    @Override
    @Transactional
    public RoomResponse createRoom(RoomRequest request, MultipartFile[] images, List<String> imageOrder) {
        log.info("Creating room: {} with {} images", request.roomNumber(),
                images != null ? images.length : 0);

        // Bước 1: Validate room number
        if (roomRepository.existsByRoomNumber(request.roomNumber())) {
            throw ErrorDefinition.DUPLICATE_ID.toAppError()
                    .withDetail("roomNumber", request.roomNumber());
        }

        // Bước 2: Validate RoomType exists
        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> ErrorDefinition.ROOM_TYPE_NOT_FOUND.toAppError()
                        .withDetail("roomTypeId", request.roomTypeId()));

        // Bước 3: Get or set default RoomStatus
        RoomStatus roomStatus;
        if (request.roomStatusId() != null) {
            roomStatus = roomStatusRepository.findById(request.roomStatusId())
                    .orElseThrow(() -> ErrorDefinition.ROOM_STATUS_NOT_FOUND.toAppError()
                            .withDetail("roomStatusId", request.roomStatusId()));
        } else {
            // ✅ Set default status "Available" nếu không được cung cấp (case-insensitive)
            roomStatus = roomStatusRepository.findByNameIgnoreCase("Available")
                    .orElseThrow(() -> ErrorDefinition.ROOM_STATUS_NOT_FOUND.toAppError()
                            .withDetail("statusName", "Available"));
            log.debug("Using default status: Available");
        }

        // Bước 4: Build Room entity với đầy đủ thông tin
        Room room = Room.builder()
                .roomNumber(request.roomNumber())
                .type(roomType)
                .status(roomStatus)
                .floor(request.floor())
                .note(request.note())
                .build();

        // Bước 5: Lưu Room trước để có ID
        Room savedRoom = roomRepository.save(room);
        log.debug("Room saved with ID: {}", savedRoom.getId());

        // Bước 6: Xử lý vòng lặp ảnh
        if (images != null && images.length > 0) {
            for (int i = 0; i < images.length; i++) {
                MultipartFile imageFile = images[i];

                // 3.1: Lưu file vào disk
                String fileName = fileStorageService.storeFile(imageFile);
                log.debug("Image file saved: {}", fileName);

                // 3.2: Tạo RoomImage entity
                RoomImage roomImage = RoomImage.builder()
                        .room(savedRoom)
                        .imageUrl("/uploads/" + fileName)
                        .isPrimary(i == 0) // Ảnh đầu tiên là primary
                        .displayOrder((short) i)
                        .build();

                // 3.3: Lưu RoomImage vào database
                roomImageRepository.save(roomImage);
                log.debug("RoomImage saved for room: {}, order: {}", savedRoom.getId(), i);
            }

            // Refresh room để load images
            savedRoom = roomRepository.findById(savedRoom.getId())
                    .orElseThrow(() -> ErrorDefinition.ROOM_NOT_FOUND.toAppError());
        }

        return roomMapper.toResponse(savedRoom);
    }

    /**
     * ✅ FLOW CHUẨN: Update room WITH new images
     */
    @Override
    @Transactional
    public RoomResponse updateRoom(String roomId, RoomRequest request, MultipartFile[] newImages,
            List<String> imageOrder) {
        log.info("Updating room: {} with {} new images", roomId,
                newImages != null ? newImages.length : 0);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> ErrorDefinition.ROOM_NOT_FOUND.toAppError().withDetail("roomId", roomId));

        if (!room.getRoomNumber().equals(request.roomNumber()) &&
                roomRepository.existsByRoomNumber(request.roomNumber())) {
            throw ErrorDefinition.DUPLICATE_ID.toAppError().withDetail("roomNumber", request.roomNumber());
        }

        // Update room info
        room.setRoomNumber(request.roomNumber());
        room.setType(RoomType.builder().id(request.roomTypeId()).build());
        if (request.roomStatusId() != null) {
            room.setStatus(RoomStatus.builder().id(request.roomStatusId()).build());
        }
        room.setFloor(request.floor());
        room.setNote(request.note());

        Room updatedRoom = roomRepository.save(room);

        // Add new images if provided
        if (newImages != null && newImages.length > 0) {
            // Get current max display order
            int currentMaxOrder = room.getImages().stream()
                    .mapToInt(RoomImage::getDisplayOrder)
                    .max()
                    .orElse(-1);

            for (int i = 0; i < newImages.length; i++) {
                MultipartFile imageFile = newImages[i];

                String fileName = fileStorageService.storeFile(imageFile);

                RoomImage roomImage = RoomImage.builder()
                        .room(updatedRoom)
                        .imageUrl("/uploads/" + fileName)
                        .isPrimary(false) // Don't override existing primary
                        .displayOrder((short) (currentMaxOrder + i + 1))
                        .build();

                roomImageRepository.save(roomImage);
            }

            // Refresh to load new images
            updatedRoom = roomRepository.findById(updatedRoom.getId())
                    .orElseThrow(() -> ErrorDefinition.ROOM_NOT_FOUND.toAppError());
        }

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

    @Override
    @Transactional(readOnly = true)
    public RoomAvailabilityResponse checkAvailability(RoomAvailabilityRequest request) {
        List<RoomAvailabilityResponse.RoomAvailabilityDetail> details = new ArrayList<>();
        boolean allAvailable = true;
        BigDecimal estimatedTotal = BigDecimal.ZERO;
        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights < 1)
            nights = 1;

        List<String> roomIds = request.roomIds();
        if (roomIds == null || roomIds.isEmpty()) {
            // If no specific rooms provided, check all rooms
            roomIds = roomRepository.findAll().stream()
                    .map(Room::getId)
                    .collect(Collectors.toList());
        }

        for (String roomId : roomIds) {
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> ErrorDefinition.ROOM_NOT_FOUND.toAppError()
                            .withDetail("roomId", roomId));

            boolean hasConflict = reservationRoomRepository.hasConflictingReservation(
                    room.getId(), request.checkIn(), request.checkOut());

            boolean isAvailable = !hasConflict;
            if (!isAvailable) {
                allAvailable = false;
            }

            BigDecimal pricePerNight = room.getType() != null && room.getType().getPricePerNight() != null
                    ? room.getType().getPricePerNight()
                    : BigDecimal.ZERO;

            if (isAvailable) {
                estimatedTotal = estimatedTotal.add(pricePerNight.multiply(BigDecimal.valueOf(nights)));
            }

            details.add(new RoomAvailabilityResponse.RoomAvailabilityDetail(
                    room.getId(),
                    room.getRoomNumber(),
                    isAvailable,
                    room.getType() != null ? room.getType().getName() : null,
                    pricePerNight));
        }

        return new RoomAvailabilityResponse(
                allAvailable,
                details,
                request.checkIn(),
                request.checkOut(),
                nights,
                estimatedTotal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        return roomRepository.findAll().stream()
                .filter(room -> !reservationRoomRepository.hasConflictingReservation(
                        room.getId(), checkIn, checkOut))
                .map(roomMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> uploadRoomImages(String roomId, List<MultipartFile> files) {
        log.debug("Uploading {} images for room ID: {}", files.size(), roomId);

        // Find room
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> ErrorDefinition.ROOM_NOT_FOUND.toAppError()
                        .withDetail("roomId", roomId));

        List<String> imageUrls = new ArrayList<>();

        // Check if this is the first image (should be primary)
        boolean isFirstImage = roomImageRepository.findByRoomId(roomId).isEmpty();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);

            // Store file and get URL
            String imageUrl = fileStorageService.storeFile(file);

            // Create RoomImage entity
            RoomImage roomImage = new RoomImage();
            roomImage.setRoom(room);
            roomImage.setImageUrl(imageUrl);
            // First image is primary, or first in this batch if no images exist
            roomImage.setIsPrimary(isFirstImage && i == 0);

            roomImageRepository.save(roomImage);
            imageUrls.add(imageUrl);

            log.info("Saved image {} for room {}", roomImage.getId(), roomId);
        }

        return imageUrls;
    }

    @Override
    public void deleteRoomImage(String roomId, String imageId) {
        log.debug("Deleting image {} for room ID: {}", imageId, roomId);

        // Find image
        RoomImage roomImage = roomImageRepository.findById(imageId)
                .orElseThrow(() -> ErrorDefinition.ROOM_IMAGE_NOT_FOUND.toAppError()
                        .withDetail("imageId", imageId));

        // Verify image belongs to this room
        if (!roomImage.getRoom().getId().equals(roomId)) {
            throw ErrorDefinition.ROOM_IMAGE_NOT_FOUND.toAppError()
                    .withDetail("message", "Image does not belong to this room");
        }

        // Delete file from storage
        String filename = fileStorageService.extractFilename(roomImage.getImageUrl());
        if (filename != null) {
            fileStorageService.deleteFile(filename);
        }

        // If this was primary image, set another image as primary
        if (roomImage.getIsPrimary()) {
            List<RoomImage> otherImages = roomImageRepository.findByRoomId(roomId).stream()
                    .filter(img -> !img.getId().equals(imageId))
                    .toList();

            if (!otherImages.isEmpty()) {
                RoomImage newPrimary = otherImages.get(0);
                newPrimary.setIsPrimary(true);
                roomImageRepository.save(newPrimary);
            }
        }

        // Delete from database
        roomImageRepository.delete(roomImage);
        log.info("Deleted image {} for room {}", imageId, roomId);
    }

    @Override
    public void setPrimaryImage(String roomId, String imageId) {
        log.debug("Setting image {} as primary for room ID: {}", imageId, roomId);

        // Find image
        RoomImage roomImage = roomImageRepository.findById(imageId)
                .orElseThrow(() -> ErrorDefinition.ROOM_IMAGE_NOT_FOUND.toAppError()
                        .withDetail("imageId", imageId));

        // Verify image belongs to this room
        if (!roomImage.getRoom().getId().equals(roomId)) {
            throw ErrorDefinition.ROOM_IMAGE_NOT_FOUND.toAppError()
                    .withDetail("message", "Image does not belong to this room");
        }

        // Unset current primary image
        List<RoomImage> roomImages = roomImageRepository.findByRoomId(roomId);
        for (RoomImage img : roomImages) {
            if (img.getIsPrimary()) {
                img.setIsPrimary(false);
                roomImageRepository.save(img);
            }
        }

        // Set new primary image
        roomImage.setIsPrimary(true);
        roomImageRepository.save(roomImage);

        log.info("Set image {} as primary for room {}", imageId, roomId);
    }
}
