package com.hotelmanagement.quanlikhachsan.exception.email;

/**
 * Exception thrown when OTP validation fails
 */
public class OTPInvalidException extends RuntimeException {
    public OTPInvalidException(String message) {
        super(message);
    }
}
