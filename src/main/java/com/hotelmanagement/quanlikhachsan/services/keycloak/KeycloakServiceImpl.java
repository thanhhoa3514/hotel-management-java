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
    @Override
    public Optional<Keycloak> findKeycloakByUserId(UUID keycloakUserId) {
        return Optional.empty();
    }

    @Override
    public Optional<Keycloak> findKeycloakByEmail(String email) {
        return Optional.empty();
    }
}
