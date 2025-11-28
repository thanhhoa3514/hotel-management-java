package com.hotelmanagement.quanlikhachsan.services.auth;

import com.hotelmanagement.quanlikhachsan.dto.request.auth.LoginRequest;
import com.hotelmanagement.quanlikhachsan.dto.request.auth.RegisterRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.auth.AuthResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.auth.UserInfo;
import com.hotelmanagement.quanlikhachsan.dto.response.guest.GuestResponse;
import com.hotelmanagement.quanlikhachsan.services.guest.IGuestService;
import com.hotelmanagement.quanlikhachsan.services.keycloak.IKeycloakService;
import com.hotelmanagement.quanlikhachsan.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService
 * Tests authentication logic with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private KeycloakAuthenticationService keycloakAuthService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private IKeycloakService keycloakService;

    @Mock
    private IGuestService guestService;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private GuestResponse guestResponse;
    private UserRepresentation keycloakUser;
    private AccessTokenResponse tokenResponse;

    @BeforeEach
    void setUp() {
        // Setup test data
        loginRequest = new LoginRequest("test@example.com", "password123");

        registerRequest = new RegisterRequest(
                "John Doe",
                "john@example.com",
                "password123",
                "0123456789",
                "123 Street");

        // Fixed: GuestResponse expects UUID for keycloakUserId, not String
        guestResponse = new GuestResponse(
                "guest-uuid-123",
                "John Doe",
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), // UUID keycloakUserId
                LocalDateTime.now(),
                LocalDateTime.now());

        keycloakUser = new UserRepresentation();
        keycloakUser.setId("550e8400-e29b-41d4-a716-446655440000");
        keycloakUser.setEmail("test@example.com");
        keycloakUser.setFirstName("John");
        keycloakUser.setLastName("Doe");

        tokenResponse = new AccessTokenResponse();
        tokenResponse.setToken("mock-keycloak-token");
    }

    @Test
    @DisplayName("Login - Success with valid credentials")
    void login_WithValidCredentials_ReturnsAuthResponse() {
        // Given
        when(keycloakAuthService.authenticateUser(anyString(), anyString()))
                .thenReturn(tokenResponse);
        when(keycloakAuthService.getUserByEmail(anyString()))
                .thenReturn(keycloakUser);
        when(guestService.getGuestByKeycloakUserId(any(UUID.class)))
                .thenReturn(guestResponse);
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("mock-jwt-token");
        when(jwtUtil.getExpirationTime())
                .thenReturn(86400L);

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(86400L, response.expiresIn());
        assertNotNull(response.user());
        assertEquals("John Doe", response.user().fullName());
        assertEquals("test@example.com", response.user().email());

        // Verify interactions
        verify(keycloakAuthService).authenticateUser("test@example.com", "password123");
        verify(keycloakAuthService).getUserByEmail("test@example.com");
        verify(guestService).getGuestByKeycloakUserId(any(UUID.class));
        verify(jwtUtil).generateToken(anyString(), anyString(), anyString(), eq("USER"));
    }

    @Test
    @DisplayName("Login - Failure with invalid credentials")
    void login_WithInvalidCredentials_ThrowsBadCredentialsException() {
        // Given
        when(keycloakAuthService.authenticateUser(anyString(), anyString()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest);
        });

        verify(keycloakAuthService).authenticateUser("test@example.com", "password123");
        verify(guestService, never()).getGuestByKeycloakUserId(any());
        verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Login - User not found in database")
    void login_WhenUserNotFoundInDatabase_ThrowsBadCredentialsException() {
        // Given
        when(keycloakAuthService.authenticateUser(anyString(), anyString()))
                .thenReturn(tokenResponse);
        when(keycloakAuthService.getUserByEmail(anyString()))
                .thenReturn(keycloakUser);
        when(guestService.getGuestByKeycloakUserId(any(UUID.class)))
                .thenThrow(new RuntimeException("Guest not found"));

        // When & Then
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest);
        });
    }

    @Test
    @DisplayName("Register - Success with valid data")
    void register_WithValidData_ReturnsAuthResponse() {
        // Given
        when(keycloakService.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");
        when(guestService.createGuest(any()))
                .thenReturn(guestResponse);
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("mock-jwt-token");
        when(jwtUtil.getExpirationTime())
                .thenReturn(86400L);

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertNotNull(response);
        assertEquals("mock-jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertNotNull(response.user());
        assertEquals("John Doe", response.user().fullName());
        assertEquals("john@example.com", response.user().email());

        // Verify interactions
        verify(keycloakService).createUser(
                eq("john@example.com"),
                eq("password123"),
                eq("John"),
                eq("Doe"));
        verify(guestService).createGuest(any());
        verify(jwtUtil).generateToken(anyString(), anyString(), eq("John Doe"), eq("USER"));
    }

    @Test
    @DisplayName("Register - Failure when Keycloak user creation fails")
    void register_WhenKeycloakCreationFails_ThrowsRuntimeException() {
        // Given
        when(keycloakService.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Keycloak error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });

        verify(guestService, never()).createGuest(any());
        verify(jwtUtil, never()).generateToken(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Register - Parse full name correctly")
    void register_ParsesFullNameIntoFirstAndLastName() {
        // Given
        RegisterRequest requestWithFullName = new RegisterRequest(
                "John Michael Doe",
                "john@example.com",
                "password123",
                null,
                null);

        when(keycloakService.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");
        when(guestService.createGuest(any()))
                .thenReturn(guestResponse);
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("mock-jwt-token");
        when(jwtUtil.getExpirationTime())
                .thenReturn(86400L);

        // When
        authService.register(requestWithFullName);

        // Then
        verify(keycloakService).createUser(
                anyString(),
                anyString(),
                eq("John"),
                eq("Michael Doe") // lastName should be everything after first name
        );
    }

    @Test
    @DisplayName("GetCurrentUser - Extract user info from token")
    void getCurrentUser_ExtractsUserInfoFromToken() {
        // Given
        String token = "valid-jwt-token";
        when(jwtUtil.extractUserId(token)).thenReturn("user-id");
        when(jwtUtil.extractEmail(token)).thenReturn("test@example.com");
        when(jwtUtil.extractFullName(token)).thenReturn("John Doe");
        when(jwtUtil.extractRole(token)).thenReturn("USER");

        // When
        UserInfo userInfo = authService.getCurrentUser(token);

        // Then
        assertNotNull(userInfo);
        assertEquals("user-id", userInfo.id());
        assertEquals("test@example.com", userInfo.email());
        assertEquals("John Doe", userInfo.fullName());
        assertEquals("USER", userInfo.role());

        verify(jwtUtil).extractUserId(token);
        verify(jwtUtil).extractEmail(token);
        verify(jwtUtil).extractFullName(token);
        verify(jwtUtil).extractRole(token);
    }

    @Test
    @DisplayName("Register - Handle single name (no last name)")
    void register_WithSingleName_UsesEmptyLastName() {
        // Given
        RegisterRequest singleNameRequest = new RegisterRequest(
                "Madonna",
                "madonna@example.com",
                "password123",
                null,
                null);

        when(keycloakService.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("550e8400-e29b-41d4-a716-446655440000");
        when(guestService.createGuest(any()))
                .thenReturn(guestResponse);
        when(jwtUtil.generateToken(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("mock-jwt-token");
        when(jwtUtil.getExpirationTime())
                .thenReturn(86400L);

        // When
        authService.register(singleNameRequest);

        // Then
        verify(keycloakService).createUser(
                anyString(),
                anyString(),
                eq("Madonna"),
                eq("") // Empty last name
        );
    }
}
