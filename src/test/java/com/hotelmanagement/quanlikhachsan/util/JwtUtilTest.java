package com.hotelmanagement.quanlikhachsan.util;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtUtil
 * Tests JWT token generation, validation, and claims extraction
 */
@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-security-purposes-in-testing";
    private static final Long TEST_EXPIRATION = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Use reflection to set private fields for testing
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
    }

    @Test
    @DisplayName("Generate Token - Creates valid JWT")
    void generateToken_CreatesValidJwt() {
        // Given
        String userId = "user-123";
        String email = "test@example.com";
        String fullName = "John Doe";
        String role = "USER";

        // When
        String token = jwtUtil.generateToken(userId, email, fullName, role);

        // Then
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts separated by dots
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    @DisplayName("Extract UserId - Returns correct userId from token")
    void extractUserId_ReturnsCorrectValue() {
        // Given
        String userId = "user-123";
        String token = jwtUtil.generateToken(userId, "test@example.com", "John Doe", "USER");

        // When
        String extractedUserId = jwtUtil.extractUserId(token);

        // Then
        assertEquals(userId, extractedUserId);
    }

    @Test
    @DisplayName("Extract Email - Returns correct email from token")
    void extractEmail_ReturnsCorrectValue() {
        // Given
        String email = "test@example.com";
        String token = jwtUtil.generateToken("user-123", email, "John Doe", "USER");

        // When
        String extractedEmail = jwtUtil.extractEmail(token);

        // Then
        assertEquals(email, extractedEmail);
    }

    @Test
    @DisplayName("Extract FullName - Returns correct fullName from token")
    void extractFullName_ReturnsCorrectValue() {
        // Given
        String fullName = "John Doe";
        String token = jwtUtil.generateToken("user-123", "test@example.com", fullName, "USER");

        // When
        String extractedFullName = jwtUtil.extractFullName(token);

        // Then
        assertEquals(fullName, extractedFullName);
    }

    @Test
    @DisplayName("Extract Role - Returns correct role from token")
    void extractRole_ReturnsCorrectValue() {
        // Given
        String role = "ADMIN";
        String token = jwtUtil.generateToken("user-123", "test@example.com", "John Doe", role);

        // When
        String extractedRole = jwtUtil.extractRole(token);

        // Then
        assertEquals(role, extractedRole);
    }

    @Test
    @DisplayName("Is Token Valid - Returns true for valid token")
    void isTokenValid_WithValidToken_ReturnsTrue() {
        // Given
        String token = jwtUtil.generateToken("user-123", "test@example.com", "John Doe", "USER");

        // When
        boolean isValid = jwtUtil.isTokenValid(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Is Token Valid - Returns false for invalid token")
    void isTokenValid_WithInvalidToken_ReturnsFalse() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtUtil.isTokenValid(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Is Token Valid - Returns false for null token")
    void isTokenValid_WithNullToken_ReturnsFalse() {
        // When & Then
        assertFalse(jwtUtil.isTokenValid(null));
    }

    @Test
    @DisplayName("Is Token Expired - Returns false for fresh token")
    void isTokenExpired_WithFreshToken_ReturnsFalse() {
        // Given
        String token = jwtUtil.generateToken("user-123", "test@example.com", "John Doe", "USER");

        // When
        boolean isExpired = jwtUtil.isTokenExpired(token);

        // Then
        assertFalse(isExpired);
    }

    @Test
    @DisplayName("Is Token Expired - Returns true for expired token")
    void isTokenExpired_WithExpiredToken_ReturnsTrue() throws InterruptedException {
        // Given - Create util with very short expiration
        JwtUtil shortExpirationUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortExpirationUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(shortExpirationUtil, "expiration", 1L); // 1ms

        String token = shortExpirationUtil.generateToken("user-123", "test@example.com", "John Doe", "USER");

        // Wait for token to expire
        Thread.sleep(10);

        // When
        boolean isExpired = shortExpirationUtil.isTokenExpired(token);

        // Then
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Get Expiration Time - Returns correct expiration in seconds")
    void getExpirationTime_ReturnsCorrectValue() {
        // When
        Long expirationTime = jwtUtil.getExpirationTime();

        // Then
        assertEquals(TEST_EXPIRATION / 1000, expirationTime); // Should be in seconds
    }

    @Test
    @DisplayName("Extract Claims - Works with special characters in data")
    void extractClaims_HandlesSpecialCharacters() {
        // Given
        String fullName = "Nguyễn Văn Tèo";
        String email = "test+special@example.com";
        String token = jwtUtil.generateToken("user-123", email, fullName, "USER");

        // When
        String extractedName = jwtUtil.extractFullName(token);
        String extractedEmail = jwtUtil.extractEmail(token);

        // Then
        assertEquals(fullName, extractedName);
        assertEquals(email, extractedEmail);
    }

    @Test
    @DisplayName("Token Generation - Different users get different tokens")
    void generateToken_DifferentUsers_ProduceDifferentTokens() {
        // Given & When
        String token1 = jwtUtil.generateToken("user-1", "user1@example.com", "User One", "USER");
        String token2 = jwtUtil.generateToken("user-2", "user2@example.com", "User Two", "USER");

        // Then
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Token Generation - Same user at different times gets different tokens")
    void generateToken_SameUser_DifferentTimes_ProduceDifferentTokens() throws InterruptedException {
        // Given
        String userId = "user-123";

        // When
        String token1 = jwtUtil.generateToken(userId, "test@example.com", "John Doe", "USER");
        Thread.sleep(100); // Small delay to ensure different timestamp
        String token2 = jwtUtil.generateToken(userId, "test@example.com", "John Doe", "USER");

        // Then
        assertNotEquals(token1, token2); // Different due to different issuedAt time
    }
}
