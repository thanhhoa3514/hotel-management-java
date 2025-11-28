package com.hotelmanagement.quanlikhachsan.dto.request.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request to verify OTP
 */
public record VerifyOTPRequest(
        @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email,

        @NotBlank(message = "OTP is required") @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits") String otp) {
}
