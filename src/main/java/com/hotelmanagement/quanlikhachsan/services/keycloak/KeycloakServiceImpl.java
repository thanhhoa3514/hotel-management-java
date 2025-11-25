package com.hotelmanagement.quanlikhachsan.services.keycloak;

import com.hotelmanagement.quanlikhachsan.model.keycloak.Keycloak;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j

public class KeycloakServiceImpl implements IKeycloakService {

    private final org.keycloak.admin.client.Keycloak keycloak;

    @org.springframework.beans.factory.annotation.Value("${keycloak.realm:hotel-realm}")
    private String realm;

    @Override
    public Optional<Keycloak> findKeycloakByUserId(UUID keycloakUserId) {
        // Implementation for finding user by ID (can use Keycloak admin client if needed, or local DB if synced)
        // For now, returning empty as per original code, or implement if needed.
        // Given the context, we might want to fetch from Keycloak or just return empty if not strictly required yet.
        return Optional.empty();
    }

    @Override
    public Optional<Keycloak> findKeycloakByEmail(String email) {
        return Optional.empty();
    }

    @Override
    public String createUser(String email, String password, String firstName, String lastName) {
        org.keycloak.representations.idm.UserRepresentation user = new org.keycloak.representations.idm.UserRepresentation();
        user.setEnabled(true);
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmailVerified(true);

        org.keycloak.representations.idm.CredentialRepresentation credential = new org.keycloak.representations.idm.CredentialRepresentation();
        credential.setType(org.keycloak.representations.idm.CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        user.setCredentials(java.util.Collections.singletonList(credential));

        jakarta.ws.rs.core.Response response = keycloak.realm(realm).users().create(user);

        if (response.getStatus() == 201) {
            String userId = org.keycloak.admin.client.CreatedResponseUtil.getCreatedId(response);
            log.info("User created in Keycloak with ID: {}", userId);
            return userId;
        } else {
            log.error("Failed to create user in Keycloak. Status: {}", response.getStatus());
            throw new RuntimeException("Failed to create user in Keycloak");
        }
    }
}
