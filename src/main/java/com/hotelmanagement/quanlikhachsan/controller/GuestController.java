package com.hotelmanagement.quanlikhachsan.controller;

import com.hotelmanagement.quanlikhachsan.dto.request.guest.GuestRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.guest.GuestResponse;
import com.hotelmanagement.quanlikhachsan.services.guest.IGuestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guests")
@RequiredArgsConstructor
public class GuestController {

    private final IGuestService guestService;

    @PostMapping
    public ResponseEntity<GuestResponse> createGuest(@Valid @RequestBody GuestRequest request) {
        GuestResponse response = guestService.createGuest(request);
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}")
    public ResponseEntity<GuestResponse> getGuestById(@org.springframework.web.bind.annotation.PathVariable java.util.UUID id) {
        GuestResponse response = guestService.getGuestById(id);
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.GetMapping
    public ResponseEntity<java.util.List<GuestResponse>> getAllGuests() {
        java.util.List<GuestResponse> response = guestService.getAllGuests();
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.GetMapping("/email/{email}")
    public ResponseEntity<GuestResponse> getGuestByEmail(@org.springframework.web.bind.annotation.PathVariable String email) {
        GuestResponse response = guestService.getGuestByEmail(email);
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.GetMapping("/keycloak/{keycloakUserId}")
    public ResponseEntity<GuestResponse> getGuestByKeycloakUserId(@org.springframework.web.bind.annotation.PathVariable java.util.UUID keycloakUserId) {
        GuestResponse response = guestService.getGuestByKeycloakUserId(keycloakUserId);
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ResponseEntity<GuestResponse> updateGuest(@org.springframework.web.bind.annotation.PathVariable String id, @Valid @RequestBody GuestRequest request) {
        GuestResponse response = guestService.updateGuest(id, request);
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuest(@org.springframework.web.bind.annotation.PathVariable String id) {
        guestService.deleteGuest(id);
        return ResponseEntity.noContent().build();
    }
}
