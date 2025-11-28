package com.hotelmanagement.quanlikhachsan.exception.email;

/**
 * Exception thrown when OTP has expired
 */
public class OTPExpiredException extends RuntimeException {
    public OTPExpiredException(String message) {
        super(message);
    }
}
