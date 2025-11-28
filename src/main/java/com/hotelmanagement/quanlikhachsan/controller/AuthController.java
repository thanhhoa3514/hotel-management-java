package com.hotelmanagement.quanlikhachsan.controller;

import com.hotelmanagement.quanlikhachsan.dto.request.auth.LoginRequest;
import com.hotelmanagement.quanlikhachsan.dto.request.auth.RegisterRequest;
import com.hotelmanagement.quanlikhachsan.dto.response.ApiResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.auth.AuthResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.auth.UserInfo;
import com.hotelmanagement.quanlikhachsan.services.auth.AuthService;
import com.hotelmanagement.quanlikhachsan.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    private static final String JWT_COOKIE_NAME = "jwt";
    private static final int COOKIE_MAX_AGE = 24 * 60 * 60; // 24 hours in seconds

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        log.info("Login attempt for user: {}", request.email());

        AuthResponse authResponse = authService.login(request);

        // Set JWT in httpOnly cookie
        Cookie jwtCookie = createJwtCookie(authResponse.accessToken());
        response.addCookie(jwtCookie);

        log.info("User logged in successfully: {}", request.email());

        // Return response without token (since it's in cookie)
        AuthResponse responseWithoutToken = new AuthResponse(
                null, // Don't send token in response body
                authResponse.tokenType(),
                authResponse.expiresIn(),
                authResponse.user());

        return ResponseEntity.ok(ApiResponse.success(responseWithoutToken));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        log.info("Registration attempt for email: {}", request.email());

        AuthResponse authResponse = authService.register(request);

        // Set JWT in httpOnly cookie
        Cookie jwtCookie = createJwtCookie(authResponse.accessToken());
        response.addCookie(jwtCookie);

        log.info("User registered successfully: {}", request.email());

        // Return response without token
        AuthResponse responseWithoutToken = new AuthResponse(
                null,
                authResponse.tokenType(),
                authResponse.expiresIn(),
                authResponse.user());

        return ResponseEntity.ok(ApiResponse.success(responseWithoutToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse response) {
        // Clear JWT cookie
        Cookie jwtCookie = new Cookie(JWT_COOKIE_NAME, null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false); // Set to true in production with HTTPS
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0); // Delete cookie

        response.addCookie(jwtCookie);

        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> getCurrentUser(HttpServletRequest request) {
        String token = extractTokenFromCookie(request);

        if (token == null || !jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Unauthorized: Token không hợp lệ"));
        }

        UserInfo userInfo = authService.getCurrentUser(token);
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    private Cookie createJwtCookie(String token) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);
        cookie.setHttpOnly(true); // Cannot be accessed by JavaScript
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE);
        // cookie.setSameSite("Strict"); // Requires Servlet 6.0+

        return cookie;
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
