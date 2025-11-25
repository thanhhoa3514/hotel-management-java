package com.hotelmanagement.quanlikhachsan.services.guest;

import com.hotelmanagement.quanlikhachsan.dto.request.guest.GuestRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.guest.GuestResponse;
import com.hotelmanagement.quanlikhachsan.mapper.GuestMapper;
import com.hotelmanagement.quanlikhachsan.model.guest.Guest;
import com.hotelmanagement.quanlikhachsan.model.keycloak.Keycloak;
import com.hotelmanagement.quanlikhachsan.repository.GuestRepository;
import com.hotelmanagement.quanlikhachsan.repository.KeycloakRepository;
import com.hotelmanagement.quanlikhachsan.services.keycloak.IKeycloakService;
import com.hotelmanagement.quanlikhachsan.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GuestServiceImpl implements IGuestService {

    private final GuestRepository guestRepository;
    private final IKeycloakService  keycloakService;
    private final GuestMapper guestMapper;
    @Override
    public GuestResponse createGuest(GuestRequest request) {
        log.debug("Creating guest for email: {}", request.email());

        // Create user in Keycloak
        String keycloakUserId = keycloakService.createUser(
                request.email(),
                request.password(),
                request.fullName(), // Assuming first name is full name for now, or split if needed
                "" // Last name empty for now
        );

        Guest guest = guestMapper.toEntity(request);
        guest.setKeycloakUserId(UUID.fromString(keycloakUserId));

        Guest savedGuest = guestRepository.save(guest);

        log.info("Guest created successfully with ID: {}", savedGuest.getId());
        return guestMapper.toResponse(savedGuest);
    }

    @Override
    /***
     *
     * @param userId not keycloakId
     * @return Guest Response
     */
    public GuestResponse getGuestById(UUID id) {
        log.debug("Fetching guest with ID: {}", id);
        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest", "id", id));
        return guestMapper.toResponse(guest);
    }

    @Override
    public GuestResponse getGuestByEmail(String email) {
        log.debug("Fetching guest with email: {}", email);
        Guest guest = guestRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Guest", "email", email));
        return guestMapper.toResponse(guest);
    }

    @Override
    public GuestResponse getGuestByKeycloakUserId(UUID keycloakUserId) {
        log.debug("Fetching guest with Keycloak User ID: {}", keycloakUserId);
        Guest guest = guestRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest", "keycloakUserId", keycloakUserId));
        return guestMapper.toResponse(guest);
    }

    @Override
    public List<GuestResponse> getAllGuests() {
        log.debug("Fetching all guests");
        return guestRepository.findAll().stream()
                .map(guestMapper::toResponse)
                .toList();
    }

    @Override
    public GuestResponse updateGuest(String id, GuestRequest request) {
        log.debug("Updating guest with ID: {}", id);
        Guest guest = guestRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResourceNotFoundException("Guest", "id", id));

        // Update fields
        guest.setFullName(request.fullName());
        guest.setEmail(request.email());
        guest.setPhone(request.phone());
        guest.setAddress(request.address());
        // Note: Password and Keycloak ID are usually not updated here directly or need special handling
        // For now, we assume basic profile update.

        Guest updatedGuest = guestRepository.save(guest);
        return guestMapper.toResponse(updatedGuest);
    }

    @Override
    public void deleteGuest(String id) {
        log.debug("Deleting guest with ID: {}", id);
        if (!guestRepository.existsById(UUID.fromString(id))) {
            throw new ResourceNotFoundException("Guest", "id", id);
        }
        guestRepository.deleteById(UUID.fromString(id));
    }

    private Optional<Keycloak> findKeycloakByUserId(UUID keycloakUserId) {
        Keycloak user = keycloakService.findKeycloakByUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "keycloakUserId", keycloakUserId));
        return Optional.of(user);
    }
    private Optional<Keycloak> findKeycloakByEmail(String emailUser){
        Keycloak user = keycloakService.findKeycloakByEmail(emailUser)
                .orElseThrow(() -> new ResourceNotFoundException("User", "keycloakUserId", emailUser));
        return Optional.of(user);
    }
}
