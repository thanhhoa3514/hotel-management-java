package com.hotelmanagement.quanlikhachsan.dto.response.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,  // "Bearer"
        Long expiresIn,    // seconds
        UserInfo user
) {
    public static AuthResponse of(String accessToken, Long expiresIn, UserInfo user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, user);
    }
}
