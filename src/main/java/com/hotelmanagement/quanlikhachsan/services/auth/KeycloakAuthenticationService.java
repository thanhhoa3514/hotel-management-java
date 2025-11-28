package com.hotelmanagement.quanlikhachsan.services.auth;

import com.hotelmanagement.quanlikhachsan.dto.response.auth.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Service responsible for Keycloak authentication operations
 * Following Single Responsibility Principle - only handles Keycloak auth
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthenticationService {

    private final Keycloak adminKeycloak;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    /**
     * Authenticate user with Keycloak and retrieve access token
     * 
     * @param email User email
     * @param password User password
     * @return AccessTokenResponse if authentication successful
     * @throws BadCredentialsException if credentials are invalid
     */
    public AccessTokenResponse authenticateUser(String email, String password) {
        try {
            // Create user-specific Keycloak instance for authentication
            Keycloak userKeycloak = KeycloakBuilder.builder()
                    .serverUrl(authServerUrl)
                    .realm(realm)
                    .clientId(clientId)
                    .username(email)
                    .password(password)
                    .grantType("password") // Resource Owner Password Credentials flow
                    .build();

            // Attempt to get access token - this validates credentials
            AccessTokenResponse tokenResponse = userKeycloak.tokenManager().getAccessToken();

            if (tokenResponse == null) {
                throw new BadCredentialsException("Authentication failed");
            }

            log.info("Successfully authenticated user: {}", email);
            return tokenResponse;

        } catch (Exception e) {
            log.error("Authentication failed for user: {}", email, e);
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    /**
     * Get user information from Keycloak by email
     * 
     * @param email User email
     * @return UserRepresentation from Keycloak
     * @throws BadCredentialsException if user not found
     */
    public UserRepresentation getUserByEmail(String email) {
        List<UserRepresentation> users = adminKeycloak.realm(realm)
                .users()
                .search(email, true); // exact match

        if (users.isEmpty()) {
            log.error("User not found in Keycloak: {}", email);
            throw new BadCredentialsException("User not found");
        }

        return users.get(0);
    }

    /**
     * Get user information from Keycloak by user ID
     * 
     * @param userId Keycloak user ID
     * @return UserRepresentation from Keycloak
     */
    public UserRepresentation getUserById(String userId) {
        return adminKeycloak.realm(realm)
                .users()
                .get(userId)
                .toRepresentation();
    }
}
