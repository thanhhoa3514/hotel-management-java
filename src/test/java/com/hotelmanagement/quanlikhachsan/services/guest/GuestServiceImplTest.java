package com.hotelmanagement.quanlikhachsan.services.guest;

import com.hotelmanagement.quanlikhachsan.dto.request.guest.GuestRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.guest.GuestResponse;
import com.hotelmanagement.quanlikhachsan.exception.ResourceNotFoundException;
import com.hotelmanagement.quanlikhachsan.mapper.GuestMapper;
import com.hotelmanagement.quanlikhachsan.model.guest.Guest;
import com.hotelmanagement.quanlikhachsan.repository.GuestRepository;
import com.hotelmanagement.quanlikhachsan.services.keycloak.IKeycloakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestServiceImplTest {

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private IKeycloakService keycloakService;

    @Mock
    private GuestMapper guestMapper;

    @InjectMocks
    private GuestServiceImpl guestService;

    private GuestRequest guestRequest;
    private Guest guest;
    private GuestResponse guestResponse;
    private UUID guestId;
    private UUID keycloakUserId;

    @BeforeEach
    void setUp() {
        guestId = UUID.randomUUID();
        keycloakUserId = UUID.randomUUID();
        
        guestRequest = new GuestRequest(
                "Test Guest",
                "test@example.com",
                "1234567890",
                "Test Address",
                "password123",
                null
        );

        guest = new Guest();
        guest.setId(guestId.toString());
        guest.setFullName("Test Guest");
        guest.setEmail("test@example.com");
        guest.setKeycloakUserId(keycloakUserId);

        guestResponse = new GuestResponse(
                guestId.toString(),
                "Test Guest",
                keycloakUserId,
                null,
                null
        );
    }

    @Test
    void createGuest_Success() {
        when(keycloakService.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(keycloakUserId.toString());
        when(guestMapper.toEntity(any(GuestRequest.class))).thenReturn(guest);
        when(guestRepository.save(any(Guest.class))).thenReturn(guest);
        when(guestMapper.toResponse(any(Guest.class))).thenReturn(guestResponse);

        GuestResponse response = guestService.createGuest(guestRequest);

        assertNotNull(response);
        assertEquals(guestId.toString(), response.id());
        verify(keycloakService).createUser(anyString(), anyString(), anyString(), anyString());
        verify(guestRepository).save(any(Guest.class));
    }

    @Test
    void getGuestById_Success() {
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(guest));
        when(guestMapper.toResponse(guest)).thenReturn(guestResponse);

        GuestResponse response = guestService.getGuestById(guestId);

        assertNotNull(response);
        assertEquals(guestId.toString(), response.id());
    }

    @Test
    void getGuestById_NotFound() {
        when(guestRepository.findById(guestId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> guestService.getGuestById(guestId));
    }

    @Test
    void getGuestByEmail_Success() {
        when(guestRepository.findByEmail("test@example.com")).thenReturn(Optional.of(guest));
        when(guestMapper.toResponse(guest)).thenReturn(guestResponse);

        GuestResponse response = guestService.getGuestByEmail("test@example.com");

        assertNotNull(response);
        assertEquals("Test Guest", response.fullName());
    }

    @Test
    void getGuestByKeycloakUserId_Success() {
        when(guestRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.of(guest));
        when(guestMapper.toResponse(guest)).thenReturn(guestResponse);

        GuestResponse response = guestService.getGuestByKeycloakUserId(keycloakUserId);

        assertNotNull(response);
        assertEquals(keycloakUserId, response.keycloakUserId());
    }

    @Test
    void getAllGuests_Success() {
        when(guestRepository.findAll()).thenReturn(List.of(guest));
        when(guestMapper.toResponse(guest)).thenReturn(guestResponse);

        List<GuestResponse> responses = guestService.getAllGuests();

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
    }

    @Test
    void updateGuest_Success() {
        when(guestRepository.findById(guestId)).thenReturn(Optional.of(guest));
        when(guestRepository.save(any(Guest.class))).thenReturn(guest);
        when(guestMapper.toResponse(any(Guest.class))).thenReturn(guestResponse);

        GuestResponse response = guestService.updateGuest(guestId.toString(), guestRequest);

        assertNotNull(response);
        verify(guestRepository).save(any(Guest.class));
    }

    @Test
    void deleteGuest_Success() {
        when(guestRepository.existsById(guestId)).thenReturn(true);

        guestService.deleteGuest(guestId.toString());

        verify(guestRepository).deleteById(guestId);
    }

    @Test
    void deleteGuest_NotFound() {
        when(guestRepository.existsById(guestId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> guestService.deleteGuest(guestId.toString()));
    }
}
