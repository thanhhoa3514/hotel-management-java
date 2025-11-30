package com.hotelmanagement.quanlikhachsan.services.guest;

import com.hotelmanagement.quanlikhachsan.dto.request.guest.GuestRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.guest.GuestResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for Guest management
 * Following Dependency Inversion Principle - depend on abstraction, not
 * concrete implementation
 */
public interface IGuestService {
    GuestResponse createGuest(GuestRequest request);

    GuestResponse getGuestById(UUID id);

    GuestResponse getGuestByEmail(String email);

    GuestResponse getGuestByKeycloakUserId(UUID keycloakUserId);

    List<GuestResponse> getAllGuests();

    GuestResponse updateGuest(String id, GuestRequest request);

    void deleteGuest(String id);
}
