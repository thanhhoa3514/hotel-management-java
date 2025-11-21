package com.hotelmanagement.quanlikhachsan.dto.response.guest;

import java.time.LocalDateTime;
import java.util.UUID;

public record GuestResponse(
        String id,
        String fullName,
        UUID keycloakUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
