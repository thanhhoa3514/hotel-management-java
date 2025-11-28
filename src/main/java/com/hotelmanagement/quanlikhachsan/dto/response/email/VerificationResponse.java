package com.hotelmanagement.quanlikhachsan.dto.response.email;

import java.time.LocalDateTime;

/**
 * Response after OTP verification
 */
public record VerificationResponse(
        boolean success,
        boolean verified,
        String message,
        String email,
        LocalDateTime timestamp) {
    public static VerificationResponse success(String email) {
        return new VerificationResponse(
                true,
                true,
                "Email verified successfully!",
                email,
                LocalDateTime.now());
    }

    public static VerificationResponse failure(String message, String email, int remainingAttempts) {
        String fullMessage = message;
        if (remainingAttempts > 0) {
            fullMessage += ". Remaining attempts: " + remainingAttempts;
        }
        return new VerificationResponse(
                false,
                false,
                fullMessage,
                email,
                LocalDateTime.now());
    }
}
