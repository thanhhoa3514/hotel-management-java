package com.hotelmanagement.quanlikhachsan.services.auth;

import com.hotelmanagement.quanlikhachsan.dto.request.auth.LoginRequest;
import com.hotelmanagement.quanlikhachsan.dto.request.auth.RegisterRequest;
import com.hotelmanagement.quanlikhachsan.dto.request.guest.GuestRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.auth.AuthResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.auth.UserInfo;
import com.hotelmanagement.quanlikhachsan.dto.response.guest.GuestResponse;
import com.hotelmanagement.quanlikhachsan.services.guest.IGuestService;
import com.hotelmanagement.quanlikhachsan.services.keycloak.IKeycloakService;
import com.hotelmanagement.quanlikhachsan.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

/**
 * Authentication Service Implementation
 * Following SOLID Principles:
 * - Single Responsibility: Coordinates authentication flow
 * - Open/Closed: Extensible through interfaces
 * - Dependency Inversion: Depends on abstractions (interfaces)
 * 
 * @author Hotel Management System
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    // Dependencies injected via constructor (Dependency Inversion Principle)
    private final KeycloakAuthenticationService keycloakAuthService;
    private final JwtUtil jwtUtil;
    private final IKeycloakService keycloakService;
    private final IGuestService guestService;

    /**
     * Authenticate user and generate JWT token
     * 
     * Flow:
     * 1. Authenticate with Keycloak
     * 2. Retrieve user from Keycloak
     * 3. Get guest info from database
     * 4. Generate JWT token
     * 
     * @param request Login credentials
     * @return AuthResponse with JWT and user info
     * @throws BadCredentialsException if authentication fails
     */
    public AuthResponse login(LoginRequest request) {
        try {
            log.info("Login attempt for user: {}", request.email());

            // Step 1: Authenticate with Keycloak
            AccessTokenResponse tokenResponse = keycloakAuthService.authenticateUser(
                    request.email(),
                    request.password());

            // Step 2: Get user info from Keycloak
            UserRepresentation keycloakUser = keycloakAuthService.getUserByEmail(request.email());
            String keycloakUserId = keycloakUser.getId();

            // Step 3: Get guest info from database or create if not exists (JIT
            // Provisioning)
            GuestResponse guest;
            try {
                guest = guestService.getGuestByKeycloakUserId(
                        java.util.UUID.fromString(keycloakUserId));
            } catch (Exception e) {
                // User exists in Keycloak but not in DB -> Create local user
                log.info("User not found in local DB, creating new guest for: {}", request.email());

                String firstName = keycloakUser.getFirstName();
                String lastName = keycloakUser.getLastName();
                String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                fullName = fullName.trim().isEmpty() ? "User" : fullName.trim();

                GuestRequest newGuestRequest = new GuestRequest(
                        fullName,
                        request.email(),
                        null, // phone
                        null, // address
                        java.util.UUID.randomUUID().toString(), // dummy password
                        java.util.UUID.fromString(keycloakUserId));

                guest = guestService.createGuest(newGuestRequest);
                log.info("Successfully JIT provisioned guest: {}", request.email());
            }

            // Step 4: Generate our own JWT token
            String jwtToken = jwtUtil.generateToken(
                    guest.id(),
                    request.email(),
                    guest.fullName(),
                    "USER");

            UserInfo userInfo = new UserInfo(
                    guest.id(),
                    guest.fullName(),
                    request.email(),
                    "USER");

            log.info("User logged in successfully: {}", request.email());
            return AuthResponse.of(jwtToken, jwtUtil.getExpirationTime(), userInfo);

        } catch (BadCredentialsException e) {
            log.warn("Login failed for user {}: {}", request.email(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login for user: {}", request.email(), e);
            throw new BadCredentialsException("Login failed. Please try again.");
        }
    }

    /**
     * Register new user in both Keycloak and database
     * 
     * Flow:
     * 1. Parse full name
     * 2. Create user in Keycloak
     * 3. Create guest in database
     * 4. Generate JWT token
     * 
     * @param request Registration data
     * @return AuthResponse with JWT and user info
     * @throws RuntimeException if registration fails
     */
    public AuthResponse register(RegisterRequest request) {
        try {
            log.info("Registration attempt for email: {}", request.email());

            // Step 1: Parse full name into firstName and lastName
            String[] nameParts = request.fullName().trim().split("\\s+", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";

            // Step 2: Create user in Keycloak
            String keycloakUserId = keycloakService.createUser(
                    request.email(),
                    request.password(),
                    firstName,
                    lastName);

            // Step 3: Create guest in database
            GuestRequest guestRequest = new GuestRequest(
                    request.fullName(),
                    request.email(),
                    request.phone(),
                    request.address(), // address from RegisterRequest (can be null)
                    request.password(),
                    java.util.UUID.fromString(keycloakUserId) // Convert String to UUID
            );

            GuestResponse guest = guestService.createGuest(guestRequest);

            // Step 4: Generate JWT token
            String jwtToken = jwtUtil.generateToken(
                    guest.id(),
                    request.email(),
                    guest.fullName(),
                    "USER");

            UserInfo userInfo = new UserInfo(
                    guest.id(),
                    guest.fullName(),
                    request.email(),
                    "USER");

            log.info("User registered successfully: {}", request.email());
            return AuthResponse.of(jwtToken, jwtUtil.getExpirationTime(), userInfo);

        } catch (Exception e) {
            log.error("Registration failed for email: {}", request.email(), e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Extract user information from JWT token
     * 
     * @param token JWT token
     * @return UserInfo extracted from token
     */
    public UserInfo getCurrentUser(String token) {
        String userId = jwtUtil.extractUserId(token);
        String email = jwtUtil.extractEmail(token);
        String fullName = jwtUtil.extractFullName(token);
        String role = jwtUtil.extractRole(token);

        return new UserInfo(userId, fullName, email, role);
    }
}
