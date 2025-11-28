package com.hotelmanagement.quanlikhachsan.exception.email;

/**
 * Exception thrown when trying to send OTP too quickly
 */
public class ResendCooldownException extends RuntimeException {
    public ResendCooldownException(String message) {
        super(message);
    }
}
