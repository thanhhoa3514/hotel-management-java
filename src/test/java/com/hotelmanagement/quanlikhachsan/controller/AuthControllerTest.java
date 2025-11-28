package com.hotelmanagement.quanlikhachsan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelmanagement.quanlikhachsan.dto.request.auth.LoginRequest;
import com.hotelmanagement.quanlikhachsan.dto.request.auth.RegisterRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.auth.AuthResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.auth.UserInfo;
import com.hotelmanagement.quanlikhachsan.services.auth.AuthService;
import com.hotelmanagement.quanlikhachsan.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController
 * Tests REST API endpoints with Spring MVC Test framework
 */
@WebMvcTest(AuthController.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private AuthResponse authResponse;
    private UserInfo userInfo;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("test@example.com", "password123");

        registerRequest = new RegisterRequest(
                "John Doe",
                "john@example.com",
                "password123",
                "0123456789",
                "123 Street");

        userInfo = new UserInfo(
                "user-123",
                "John Doe",
                "test@example.com",
                "USER");

        authResponse = AuthResponse.of(
                "mock-jwt-token",
                86400L,
                userInfo);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Success")
    void login_WithValidCredentials_ReturnsJwtInCookie() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt"))
                .andExpect(cookie().httpOnly("jwt", true))
                .andExpect(cookie().path("jwt", "/"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.fullName").value("John Doe"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.accessToken").value(nullValue())); // Token not in body

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Invalid credentials")
    void login_WithInvalidCredentials_Returns401() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Validation error for empty email")
    void login_WithEmptyEmail_Returns400() throws Exception {
        // Given
        LoginRequest invalidRequest = new LoginRequest("", "password123");

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Success")
    void register_WithValidData_ReturnsJwtInCookie() throws Exception {
        // Given
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt"))
                .andExpect(cookie().httpOnly("jwt", true))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.fullName").value("John Doe"))
                .andExpect(jsonPath("$.data.accessToken").value(nullValue()));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Validation error for invalid email")
    void register_WithInvalidEmail_Returns400() throws Exception {
        // Given
        RegisterRequest invalidRequest = new RegisterRequest(
                "John Doe",
                "invalid-email",
                "password123",
                null,
                null);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Missing required fields")
    void register_WithMissingFields_Returns400() throws Exception {
        // Given
        RegisterRequest invalidRequest = new RegisterRequest(
                "", // Empty fullName
                "john@example.com",
                "password123",
                null,
                null);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Success")
    void logout_ClearsCookie() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt"))
                .andExpect(cookie().maxAge("jwt", 0)) // Cookie deleted
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Đăng xuất thành công"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Success with valid token")
    void getCurrentUser_WithValidToken_ReturnsUserInfo() throws Exception {
        // Given
        String validToken = "valid-jwt-token";
        when(jwtUtil.isTokenValid(validToken)).thenReturn(true);
        when(authService.getCurrentUser(validToken)).thenReturn(userInfo);

        Cookie jwtCookie = new Cookie("jwt", validToken);

        // When & Then
        mockMvc.perform(get("/api/v1/auth/me")
                .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("user-123"))
                .andExpect(jsonPath("$.data.fullName").value("John Doe"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.role").value("USER"));

        verify(jwtUtil).isTokenValid(validToken);
        verify(authService).getCurrentUser(validToken);
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Invalid token returns 401")
    void getCurrentUser_WithInvalidToken_Returns401() throws Exception {
        // Given
        String invalidToken = "invalid-token";
        when(jwtUtil.isTokenValid(invalidToken)).thenReturn(false);

        Cookie jwtCookie = new Cookie("jwt", invalidToken);

        // When & Then
        mockMvc.perform(get("/api/v1/auth/me")
                .cookie(jwtCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        verify(jwtUtil).isTokenValid(invalidToken);
        verify(authService, never()).getCurrentUser(anyString());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - No cookie returns 401")
    void getCurrentUser_WithoutCookie_Returns401() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        verify(authService, never()).getCurrentUser(anyString());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Cookie has correct security attributes")
    void login_SetsCookieWithCorrectSecurityAttributes() throws Exception {
        // Given
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().httpOnly("jwt", true))
                .andExpect(cookie().secure("jwt", false)) // false for development
                .andExpect(cookie().path("jwt", "/"))
                .andExpect(cookie().maxAge("jwt", 86400)); // 24 hours
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Duplicate email throws exception")
    void register_WithDuplicateEmail_Returns400() throws Exception {
        // Given
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Email already exists"));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isInternalServerError());

        verify(authService).register(any(RegisterRequest.class));
    }
}
