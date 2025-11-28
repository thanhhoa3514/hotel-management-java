package com.hotelmanagement.quanlikhachsan.dto.request.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to send OTP for email verification
 */
public record SendOTPRequest(
        @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email,

        @NotBlank(message = "Full name is required") String fullName) {
}
