package com.hotelmanagement.quanlikhachsan.dto.request.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to resend OTP
 */
public record ResendOTPRequest(
        @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email) {
}
