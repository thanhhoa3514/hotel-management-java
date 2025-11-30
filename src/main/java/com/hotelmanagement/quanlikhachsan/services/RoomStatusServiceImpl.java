package com.hotelmanagement.quanlikhachsan.services;

import com.hotelmanagement.quanlikhachsan.dto.response.RoomStatusResponse;
import com.hotelmanagement.quanlikhachsan.exception.ErrorDefinition;
import com.hotelmanagement.quanlikhachsan.mapper.RoomStatusMapper;
import com.hotelmanagement.quanlikhachsan.model.room.RoomStatus;
import com.hotelmanagement.quanlikhachsan.repository.RoomStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RoomStatusServiceImpl implements IRoomStatusService {

    private final RoomStatusRepository roomStatusRepository;
    private final RoomStatusMapper roomStatusMapper;

    @Override
    public List<RoomStatusResponse> getAllRoomStatuses() {
        log.debug("Fetching all room statuses");
        List<RoomStatus> roomStatuses = roomStatusRepository.findAll();
        return roomStatuses.stream()
                .map(roomStatusMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RoomStatusResponse getRoomStatusById(String id) {
        log.debug("Fetching room status with ID: {}", id);
        RoomStatus roomStatus = roomStatusRepository.findById(id)
                .orElseThrow(() -> ErrorDefinition.ROOM_STATUS_NOT_FOUND.toAppError()
                        .withDetail("id", id));
        return roomStatusMapper.toResponse(roomStatus);
    }

    @Override
    public RoomStatusResponse getRoomStatusByName(String name) {
        log.debug("Fetching room status with name: {}", name);
        // Try case-insensitive search first
        RoomStatus roomStatus = roomStatusRepository.findByNameIgnoreCase(name)
                .or(() -> roomStatusRepository.findByName(name))
                .orElseThrow(() -> ErrorDefinition.ROOM_STATUS_NOT_FOUND.toAppError()
                        .withDetail("name", name));
        return roomStatusMapper.toResponse(roomStatus);
    }
}
