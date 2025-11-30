package com.hotelmanagement.quanlikhachsan.services;

import com.hotelmanagement.quanlikhachsan.dto.response.RoomTypeResponse;
import com.hotelmanagement.quanlikhachsan.exception.ErrorDefinition;
import com.hotelmanagement.quanlikhachsan.mapper.RoomTypeMapper;
import com.hotelmanagement.quanlikhachsan.model.room.RoomType;
import com.hotelmanagement.quanlikhachsan.repository.RoomTypeRepository;
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
public class RoomTypeServiceImpl implements IRoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeMapper roomTypeMapper;

    @Override
    public List<RoomTypeResponse> getAllRoomTypes() {
        log.debug("Fetching all room types");
        List<RoomType> roomTypes = roomTypeRepository.findAll();
        return roomTypes.stream()
                .map(roomTypeMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RoomTypeResponse getRoomTypeById(String id) {
        log.debug("Fetching room type with ID: {}", id);
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> ErrorDefinition.ROOM_TYPE_NOT_FOUND.toAppError()
                        .withDetail("id", id));
        return roomTypeMapper.toResponse(roomType);
    }

    @Override
    public RoomTypeResponse getRoomTypeByName(String name) {
        log.debug("Fetching room type with name: {}", name);
        RoomType roomType = roomTypeRepository.findByName(name)
                .orElseThrow(() -> ErrorDefinition.ROOM_TYPE_NOT_FOUND.toAppError()
                        .withDetail("name", name));
        return roomTypeMapper.toResponse(roomType);
    }
}

