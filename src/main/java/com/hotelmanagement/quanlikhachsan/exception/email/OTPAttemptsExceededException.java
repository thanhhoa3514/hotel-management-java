package com.hotelmanagement.quanlikhachsan.exception.email;

/**
 * Exception thrown when maximum OTP attempts exceeded
 */
public class OTPAttemptsExceededException extends RuntimeException {
    public OTPAttemptsExceededException(String message) {
        super(message);
    }
}
