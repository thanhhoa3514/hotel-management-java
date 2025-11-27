package com.hotelmanagement.quanlikhachsan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelmanagement.quanlikhachsan.dto.request.guest.GuestRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.guest.GuestResponse;
import com.hotelmanagement.quanlikhachsan.services.guest.IGuestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GuestController.class)
class GuestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IGuestService guestService;

    @Autowired
    private ObjectMapper objectMapper;

    private GuestRequest guestRequest;
    private GuestResponse guestResponse;
    private UUID guestId;

    @BeforeEach
    void setUp() {
        guestId = UUID.randomUUID();
        guestRequest = new GuestRequest(
                "Test Guest",
                "test@example.com",
                "1234567890",
                "Test Address",
                "password123",
                null
        );

        guestResponse = new GuestResponse(
                guestId.toString(),
                "Test Guest",
                UUID.randomUUID(),
                null,
                null
        );
    }

    @Test
    void createGuest_Success() throws Exception {
        when(guestService.createGuest(any(GuestRequest.class))).thenReturn(guestResponse);

        mockMvc.perform(post("/api/v1/guests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(guestId.toString()))
                .andExpect(jsonPath("$.fullName").value("Test Guest"));
    }

    @Test
    void getGuestById_Success() throws Exception {
        when(guestService.getGuestById(guestId)).thenReturn(guestResponse);

        mockMvc.perform(get("/api/v1/guests/{id}", guestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(guestId.toString()));
    }

    @Test
    void getAllGuests_Success() throws Exception {
        when(guestService.getAllGuests()).thenReturn(List.of(guestResponse));

        mockMvc.perform(get("/api/v1/guests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(guestId.toString()));
    }

    @Test
    void getGuestByEmail_Success() throws Exception {
        when(guestService.getGuestByEmail("test@example.com")).thenReturn(guestResponse);

        mockMvc.perform(get("/api/v1/guests/email/{email}", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Test Guest"));
    }

    @Test
    void updateGuest_Success() throws Exception {
        when(guestService.updateGuest(eq(guestId.toString()), any(GuestRequest.class))).thenReturn(guestResponse);

        mockMvc.perform(put("/api/v1/guests/{id}", guestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(guestRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(guestId.toString()));
    }

    @Test
    void deleteGuest_Success() throws Exception {
        doNothing().when(guestService).deleteGuest(guestId.toString());

        mockMvc.perform(delete("/api/v1/guests/{id}", guestId))
                .andExpect(status().isNoContent());
    }
}
