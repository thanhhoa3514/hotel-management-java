package com.hotelmanagement.quanlikhachsan.services.email;

import com.hotelmanagement.quanlikhachsan.exception.email.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Email Verification Service
 * 
 * Orchestrates the email verification flow:
 * 1. Generate and send OTP
 * 2. Validate OTP
 * 3. Update user verification status
 * 
 * Integrates OTPService and EmailService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final OTPService otpService;
    private final EmailService emailService;

    @Value("${email.verification.expiration-minutes}")
    private long expirationMinutes;

    /**
     * Send OTP to user's email
     * 
     * @param email    User email
     * @param fullName User's full name
     * @return Expiration time in seconds
     * @throws ResendCooldownException if resend too quickly
     */
    public long sendOTP(String email, String fullName) {
        log.info("Sending OTP to email: {}", email);

        // Check cooldown
        if (!otpService.canResendOTP(email)) {
            log.warn("Resend cooldown active for email: {}", email);
            throw new ResendCooldownException("Please wait before requesting another OTP");
        }

        // Generate OTP
        String otp = otpService.generateOTP(email);

        // Send email
        emailService.sendVerificationEmail(email, otp, fullName, expirationMinutes);

        // Return expiration time
        return expirationMinutes * 60; // Convert to seconds
    }

    /**
     * Verify OTP for email
     * 
     * @param email User email
     * @param otp   OTP to verify
     * @return true if verified successfully
     * @throws OTPExpiredException          if OTP expired
     * @throws OTPInvalidException          if OTP invalid
     * @throws OTPAttemptsExceededException if max attempts exceeded
     */
    public boolean verifyOTP(String email, String otp) {
        log.info("Verifying OTP for email: {}", email);

        try {
            boolean isValid = otpService.validateOTP(email, otp);

            if (isValid) {
                log.info("OTP verified successfully for email: {}", email);
                // Send welcome email asynchronously
                try {
                    emailService.sendWelcomeEmail(email, extractNameFromEmail(email));
                } catch (Exception e) {
                    log.error("Failed to send welcome email", e);
                    // Don't fail verification if welcome email fails
                }
                return true;
            } else {
                int remainingAttempts = otpService.getRemainingAttempts(email);
                log.warn("Invalid OTP for email: {}. Remaining attempts: {}", email, remainingAttempts);
                throw new OTPInvalidException("Invalid OTP code");
            }
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Maximum verification attempts exceeded")) {
                throw new OTPAttemptsExceededException(e.getMessage());
            }
            throw e;
        }
    }

    /**
     * Check if email is already verified
     * 
     * @param email User email
     * @return true if verified
     */
    public boolean isEmailVerified(String email) {
        return otpService.isVerified(email);
    }

    /**
     * Get remaining attempts for email
     * 
     * @param email User email
     * @return remaining attempts
     */
    public int getRemainingAttempts(String email) {
        return otpService.getRemainingAttempts(email);
    }

    /**
     * Get remaining expiration time for OTP
     * 
     * @param email User email
     * @return remaining seconds
     */
    public long getRemainingExpirationTime(String email) {
        return otpService.getRemainingExpirationTime(email);
    }

    /**
     * Extract name from email (fallback if fullName not provided)
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
