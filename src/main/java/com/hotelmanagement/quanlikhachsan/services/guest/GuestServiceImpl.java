package com.hotelmanagement.quanlikhachsan.services.guest;

import com.hotelmanagement.quanlikhachsan.dto.request.guest.GuestRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.guest.GuestResponse;
import com.hotelmanagement.quanlikhachsan.mapper.GuestMapper;
import com.hotelmanagement.quanlikhachsan.model.guest.Guest;
import com.hotelmanagement.quanlikhachsan.model.keycloak.Keycloak;
import com.hotelmanagement.quanlikhachsan.repository.GuestRepository;
import com.hotelmanagement.quanlikhachsan.repository.KeycloakRepository;
import com.hotelmanagement.quanlikhachsan.services.keycloak.IKeycloakService;
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
        log.debug("Creating guest for user ID: {}", request.keycloakUserId());
        Optional<Keycloak> keycloak=findKeycloakByEmail(request.email());
        log.debug("Logging for user's email: {}", request.email());


        keycloak=findKeycloakByUserId(request.keycloakUserId());


        Guest guest = guestMapper.toEntity(request);

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
        return null;
    }

    @Override
    public GuestResponse getGuestByKeycloakUserId(UUID keycloakUserId) {
        return null;
    }

    @Override
    public List<GuestResponse> getAllGuests() {
        return List.of();
    }

    @Override
    public GuestResponse updateGuest(String id, GuestRequest request) {
        return null;
    }

    @Override
    public void deleteGuest(String id) {

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
