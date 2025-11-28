package com.hotelmanagement.quanlikhachsan.security;

import com.hotelmanagement.quanlikhachsan.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter
 * Tests JWT extraction from cookies and authentication flow
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal - Valid JWT in cookie sets authentication")
    void doFilterInternal_WithValidJwtInCookie_SetsAuthentication() throws ServletException, IOException {
        // Given
        String validToken = "valid-jwt-token";
        String userId = "user-123";
        String email = "test@example.com";
        String role = "USER";

        Cookie jwtCookie = new Cookie("jwt", validToken);
        when(request.getCookies()).thenReturn(new Cookie[] { jwtCookie });
        when(jwtUtil.isTokenValid(validToken)).thenReturn(true);
        when(jwtUtil.extractUserId(validToken)).thenReturn(userId);
        when(jwtUtil.extractEmail(validToken)).thenReturn(email);
        when(jwtUtil.extractRole(validToken)).thenReturn(role);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(email, SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).isTokenValid(validToken);
    }

    @Test
    @DisplayName("doFilterInternal - No cookies continues filter chain")
    void doFilterInternal_WithNoCookies_ContinuesFilterChain() throws ServletException, IOException {
        // Given
        when(request.getCookies()).thenReturn(null);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).isTokenValid(anyString());
    }

    @Test
    @DisplayName("doFilterInternal - Invalid JWT does not set authentication")
    void doFilterInternal_WithInvalidJwt_DoesNotSetAuthentication() throws ServletException, IOException {
        // Given
        String invalidToken = "invalid-token";
        Cookie jwtCookie = new Cookie("jwt", invalidToken);

        when(request.getCookies()).thenReturn(new Cookie[] { jwtCookie });
        when(jwtUtil.isTokenValid(invalidToken)).thenReturn(false);

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).isTokenValid(invalidToken);
        verify(jwtUtil, never()).extractUserId(anyString());
    }

    @Test
    @DisplayName("doFilterInternal - No JWT cookie continues without authentication")
    void doFilterInternal_WithoutJwtCookie_ContinuesWithoutAuth() throws ServletException, IOException {
        // Given
        Cookie otherCookie = new Cookie("other", "value");
        when(request.getCookies()).thenReturn(new Cookie[] { otherCookie });

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).isTokenValid(anyString());
    }

    @Test
    @DisplayName("doFilterInternal - Multiple cookies extracts JWT correctly")
    void doFilterInternal_WithMultipleCookies_ExtractsJwtCorrectly() throws ServletException, IOException {
        // Given
        String validToken = "valid-jwt-token";
        Cookie cookie1 = new Cookie("session", "session-value");
        Cookie jwtCookie = new Cookie("jwt", validToken);
        Cookie cookie3 = new Cookie("preference", "dark-mode");

        when(request.getCookies()).thenReturn(new Cookie[] { cookie1, jwtCookie, cookie3 });
        when(jwtUtil.isTokenValid(validToken)).thenReturn(true);
        when(jwtUtil.extractUserId(validToken)).thenReturn("user-123");
        when(jwtUtil.extractEmail(validToken)).thenReturn("test@example.com");
        when(jwtUtil.extractRole(validToken)).thenReturn("USER");

        // When
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtUtil).isTokenValid(validToken);
    }

    @Test
    @DisplayName("doFilterInternal - Exception during extraction continues filter chain")
    void doFilterInternal_WithExceptionDuringExtraction_ContinuesFilterChain() throws ServletException, IOException {
        // Given
        String token = "problematic-token";
        Cookie jwtCookie = new Cookie("jwt", token);

        when(request.getCookies()).thenReturn(new Cookie[] { jwtCookie });
        when(jwtUtil.isTokenValid(token)).thenThrow(new RuntimeException("Token parsing error"));

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        });

        verify(filterChain).doFilter(request, response);
    }
}
