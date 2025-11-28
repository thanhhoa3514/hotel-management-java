package com.hotelmanagement.quanlikhachsan.controller;

import com.hotelmanagement.quanlikhachsan.dto.request.email.*;
import com.hotelmanagement.quanlikhachsan.dto.response.ApiResponse;
import com.hotelmanagement.quanlikhachsan.dto.response.email.*;
import com.hotelmanagement.quanlikhachsan.exception.email.*;
import com.hotelmanagement.quanlikhachsan.services.email.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Email Verification Controller
 * 
 * Endpoints for email verification flow:
 * - Send OTP
 * - Verify OTP
 * - Resend OTP
 * - Check verification status
 */
@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@Slf4j
public class EmailVerificationController {

    private final EmailVerificationService verificationService;

    /**
     * Send OTP to user's email
     * 
     * POST /api/v1/email/send-otp
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<OTPResponse>> sendOTP(
            @Valid @RequestBody SendOTPRequest request) {
        try {
            log.info("Sending OTP to email: {}", request.email());

            long expiresInSeconds = verificationService.sendOTP(request.email(), request.fullName());
            int remainingAttempts = verificationService.getRemainingAttempts(request.email());

            OTPResponse response = OTPResponse.success(
                    request.email(),
                    expiresInSeconds,
                    remainingAttempts);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (ResendCooldownException e) {
            log.warn("Resend cooldown for email: {}", request.email());
            OTPResponse errorResponse = OTPResponse.error(e.getMessage(), request.email());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(errorResponse, e.getMessage()));

        } catch (Exception e) {
            log.error("Error sending OTP to email: {}", request.email(), e);
            OTPResponse errorResponse = OTPResponse.error("Failed to send OTP", request.email());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(errorResponse, "An error occurred while sending OTP"));
        }
    }

    /**
     * Verify OTP
     * 
     * POST /api/v1/email/verify-otp
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<VerificationResponse>> verifyOTP(
            @Valid @RequestBody VerifyOTPRequest request) {
        try {
            log.info("Verifying OTP for email: {}", request.email());

            boolean isVerified = verificationService.verifyOTP(request.email(), request.otp());

            if (isVerified) {
                VerificationResponse response = VerificationResponse.success(request.email());
                return ResponseEntity.ok(ApiResponse.success(response));
            } else {
                // This shouldn't happen as validateOTP throws exceptions
                int remainingAttempts = verificationService.getRemainingAttempts(request.email());
                VerificationResponse response = VerificationResponse.failure(
                        "Invalid OTP code",
                        request.email(),
                        remainingAttempts);
                return ResponseEntity.badRequest().body(ApiResponse.error(response, "Invalid OTP"));
            }

        } catch (OTPExpiredException e) {
            log.warn("OTP expired for email: {}", request.email());
            VerificationResponse response = VerificationResponse.failure(
                    e.getMessage(),
                    request.email(),
                    0);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(response, "OTP has expired"));

        } catch (OTPInvalidException e) {
            log.warn("Invalid OTP for email: {}", request.email());
            int remainingAttempts = verificationService.getRemainingAttempts(request.email());
            VerificationResponse response = VerificationResponse.failure(
                    e.getMessage(),
                    request.email(),
                    remainingAttempts);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(response, "Invalid OTP code"));

        } catch (OTPAttemptsExceededException e) {
            log.warn("Max attempts exceeded for email: {}", request.email());
            VerificationResponse response = VerificationResponse.failure(
                    e.getMessage(),
                    request.email(),
                    0);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(response, "Maximum attempts exceeded"));

        } catch (Exception e) {
            log.error("Error verifying OTP for email: {}", request.email(), e);
            VerificationResponse response = VerificationResponse.failure(
                    "Verification failed",
                    request.email(),
                    0);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(response, "An error occurred during verification"));
        }
    }

    /**
     * Resend OTP
     * 
     * POST /api/v1/email/resend-otp
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<OTPResponse>> resendOTP(
            @Valid @RequestBody ResendOTPRequest request) {
        try {
            log.info("Resending OTP to email: {}", request.email());

            // Use email username as fallback for fullName
            String fullName = extractNameFromEmail(request.email());

            long expiresInSeconds = verificationService.sendOTP(request.email(), fullName);
            int remainingAttempts = verificationService.getRemainingAttempts(request.email());

            OTPResponse response = OTPResponse.success(
                    request.email(),
                    expiresInSeconds,
                    remainingAttempts);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (ResendCooldownException e) {
            log.warn("Resend cooldown for email: {}", request.email());
            OTPResponse errorResponse = OTPResponse.error(e.getMessage(), request.email());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(errorResponse, e.getMessage()));

        } catch (Exception e) {
            log.error("Error resending OTP to email: {}", request.email(), e);
            OTPResponse errorResponse = OTPResponse.error("Failed to resend OTP", request.email());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(errorResponse, "An error occurred while resending OTP"));
        }
    }

    /**
     * Check email verification status
     * 
     * GET /api/v1/email/status/{email}
     */
    @GetMapping("/status/{email}")
    public ResponseEntity<ApiResponse<Boolean>> checkStatus(@PathVariable String email) {
        try {
            log.debug("Checking verification status for email: {}", email);
            boolean isVerified = verificationService.isEmailVerified(email);
            return ResponseEntity.ok(ApiResponse.success(isVerified));

        } catch (Exception e) {
            log.error("Error checking status for email: {}", email, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(false, "Failed to check verification status"));
        }
    }

    /**
     * Extract name from email
     */
    private String extractNameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            String username = email.substring(0, atIndex);
            return username.substring(0, 1).toUpperCase() + username.substring(1);
        }
        return "User";
    }
}
