package com.hotelmanagement.quanlikhachsan.dto.response.email;

import java.time.LocalDateTime;

/**
 * Response after sending OTP
 */
public record OTPResponse(
        boolean success,
        String message,
        String email,
        long expiresInSeconds,
        int remainingAttempts,
        LocalDateTime timestamp) {
    public static OTPResponse success(String email, long expiresInSeconds, int remainingAttempts) {
        return new OTPResponse(
                true,
                "OTP sent successfully to " + maskEmail(email),
                email,
                expiresInSeconds,
                remainingAttempts,
                LocalDateTime.now());
    }

    public static OTPResponse error(String message, String email) {
        return new OTPResponse(
                false,
                message,
                email,
                0,
                0,
                LocalDateTime.now());
    }

    private static String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex > 2) {
            String masked = email.substring(0, 2) + "***" + email.substring(atIndex);
            return masked;
        }
        return email;
    }
}
