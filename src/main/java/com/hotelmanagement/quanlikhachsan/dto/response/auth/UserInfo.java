package com.hotelmanagement.quanlikhachsan.dto.response.auth;

public record UserInfo(
        String id,
        String fullName,
        String email,
        String role
) {
}
