package com.hotelmanagement.quanlikhachsan.services.keycloak;

import com.hotelmanagement.quanlikhachsan.model.keycloak.Keycloak;

import java.util.Optional;
import java.util.UUID;

public interface IKeycloakService {
    Optional<Keycloak> findKeycloakByUserId(UUID keycloakUserId);

    Optional<Keycloak> findKeycloakByEmail(String email);

    String createUser(String email, String password, String firstName, String lastName);
}
