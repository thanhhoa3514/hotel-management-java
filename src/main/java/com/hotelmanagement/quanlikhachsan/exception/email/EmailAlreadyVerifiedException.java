package com.hotelmanagement.quanlikhachsan.exception.email;

/**
 * Exception thrown when email is already verified
 */
public class EmailAlreadyVerifiedException extends RuntimeException {
    public EmailAlreadyVerifiedException(String message) {
        super(message);
    }
}
