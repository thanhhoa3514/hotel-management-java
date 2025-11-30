package com.hotelmanagement.quanlikhachsan.mapper;

import com.hotelmanagement.quanlikhachsan.dto.request.guest.GuestRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.guest.GuestResponse;
import com.hotelmanagement.quanlikhachsan.model.guest.Guest;
import org.springframework.stereotype.Component;

/**
 * Mapper for Guest entity - Following Single Responsibility Principle
 */
@Component
public class GuestMapper {

    /**
     *
     * @param request
     * @return Guest Entity
     */
    public Guest toEntity(GuestRequest request) {
        return Guest.builder()
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .keycloakUserId(request.keycloakUserId())
                .build();
    }

    /**
     *
     * @param guest
     * @param request
     *                update Guest Information with new request
     */
    public void updateEntity(Guest guest, GuestRequest request) {
        guest.setFullName(request.fullName());
        guest.setEmail(request.email());
        guest.setPhone(request.phone());
        guest.setAddress(request.address());
        if (request.keycloakUserId() != null) {
            guest.setKeycloakUserId(request.keycloakUserId());
        }
    }

    /**
     *
     * @param guest
     * @return Guest Response with full details information
     */
    public GuestResponse toResponse(Guest guest) {
        return new GuestResponse(
                guest.getId().toString(),
                guest.getFullName(),
                guest.getKeycloakUserId(),
                guest.getCreatedAt(),
                guest.getUpdatedAt());
    }
}
