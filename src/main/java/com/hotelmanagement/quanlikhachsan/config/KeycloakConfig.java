package com.hotelmanagement.quanlikhachsan.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {

    @Value("${keycloak.auth-server-url:http://localhost:8180}")
    private String authServerUrl;

    @Value("${keycloak.admin.realm:master}")
    private String realm;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String clientId;

    @Value("${keycloak.credentials.username:admin}")
    private String username;

    @Value("${keycloak.credentials.password:admin123}")
    private String password;

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .clientId(clientId)
                .username(username)
                .password(password)
                .build();
    }
}
