package com.hotelmanagement.quanlikhachsan.services.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * OTP Service for Email Verification
 * 
 * Handles OTP generation, storage, validation using Redis cache
 * Implements security best practices:
 * - Cryptographically secure random generation
 * - Automatic expiration
 * - Attempt limiting
 * - Resend cooldown
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OTPService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${email.verification.otp-length}")
    private int otpLength;

    @Value("${email.verification.expiration-minutes}")
    private long expirationMinutes;

    @Value("${email.verification.max-attempts}")
    private int maxAttempts;

    @Value("${email.verification.resend-cooldown-seconds}")
    private long resendCooldownSeconds;

    private static final String OTP_PREFIX = "otp:";
    private static final String ATTEMPTS_PREFIX = "otp:attempts:";
    private static final String COOLDOWN_PREFIX = "otp:cooldown:";
    private static final String VERIFIED_PREFIX = "verified:";

    /**
     * Generate a new OTP for the given email
     * 
     * @param email User email
     * @return Generated OTP string
     */
    public String generateOTP(String email) {
        // Generate 6-digit OTP using SecureRandom
        int otp = SECURE_RANDOM.nextInt((int) Math.pow(10, otpLength));
        String otpString = String.format("%0" + otpLength + "d", otp);

        // Store in Redis with expiration
        String key = OTP_PREFIX + email;
        redisTemplate.opsForValue().set(key, otpString, expirationMinutes, TimeUnit.MINUTES);

        // Reset attempts counter
        String attemptsKey = ATTEMPTS_PREFIX + email;
        redisTemplate.opsForValue().set(attemptsKey, 0, expirationMinutes, TimeUnit.MINUTES);

        // Set resend cooldown
        String cooldownKey = COOLDOWN_PREFIX + email;
        redisTemplate.opsForValue().set(cooldownKey, System.currentTimeMillis(),
                resendCooldownSeconds, TimeUnit.SECONDS);

        log.info("OTP generated for email: {} (expires in {} minutes)", email, expirationMinutes);
        return otpString;
    }

    /**
     * Validate OTP for the given email
     * 
     * @param email User email
     * @param otp   OTP to validate
     * @return true if OTP is valid, false otherwise
     * @throws RuntimeException if max attempts exceeded
     */
    public boolean validateOTP(String email, String otp) {
        String key = OTP_PREFIX + email;
        String attemptsKey = ATTEMPTS_PREFIX + email;

        // Check if OTP exists
        String storedOTP = (String) redisTemplate.opsForValue().get(key);
        if (storedOTP == null) {
            log.warn("OTP not found or expired for email: {}", email);
            return false;
        }

        // Check attempts
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptsKey);
        if (attempts == null) {
            attempts = 0;
        }

        if (attempts >= maxAttempts) {
            log.warn("Max OTP attempts ({}) exceeded for email: {}", maxAttempts, email);
            // Invalidate OTP
            invalidateOTP(email);
            throw new RuntimeException("Maximum verification attempts exceeded. Please request a new OTP.");
        }

        // Increment attempts
        redisTemplate.opsForValue().increment(attemptsKey);

        // Validate OTP
        boolean isValid = storedOTP.equals(otp);

        if (isValid) {
            log.info("OTP validated successfully for email: {}", email);
            // Mark as verified
            markAsVerified(email);
            // Clean up OTP data
            invalidateOTP(email);
        } else {
            log.warn("Invalid OTP attempt for email: {}. Attempts: {}/{}", email, attempts + 1, maxAttempts);
        }

        return isValid;
    }

    /**
     * Invalidate OTP and related data for email
     * 
     * @param email User email
     */
    public void invalidateOTP(String email) {
        String otpKey = OTP_PREFIX + email;
        String attemptsKey = ATTEMPTS_PREFIX + email;

        redisTemplate.delete(otpKey);
        redisTemplate.delete(attemptsKey);

        log.debug("OTP invalidated for email: {}", email);
    }

    /**
     * Check if user can resend OTP (not in cooldown period)
     * 
     * @param email User email
     * @return true if can resend, false if in cooldown
     */
    public boolean canResendOTP(String email) {
        String cooldownKey = COOLDOWN_PREFIX + email;
        Long lastSentTime = (Long) redisTemplate.opsForValue().get(cooldownKey);

        if (lastSentTime == null) {
            return true;
        }

        long elapsedSeconds = (System.currentTimeMillis() - lastSentTime) / 1000;
        boolean canResend = elapsedSeconds >= resendCooldownSeconds;

        if (!canResend) {
            long remainingSeconds = resendCooldownSeconds - elapsedSeconds;
            log.debug("Resend cooldown active for email: {}. Remaining: {} seconds", email, remainingSeconds);
        }

        return canResend;
    }

    /**
     * Get remaining attempts for email verification
     * 
     * @param email User email
     * @return remaining attempts count
     */
    public int getRemainingAttempts(String email) {
        String attemptsKey = ATTEMPTS_PREFIX + email;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptsKey);

        if (attempts == null) {
            return maxAttempts;
        }

        return Math.max(0, maxAttempts - attempts);
    }

    /**
     * Mark email as verified
     * 
     * @param email User email
     */
    private void markAsVerified(String email) {
        String verifiedKey = VERIFIED_PREFIX + email;
        redisTemplate.opsForValue().set(verifiedKey, System.currentTimeMillis(),
                1, TimeUnit.HOURS);
        log.info("Email marked as verified: {}", email);
    }

    /**
     * Check if email is verified
     * 
     * @param email User email
     * @return true if verified, false otherwise
     */
    public boolean isVerified(String email) {
        String verifiedKey = VERIFIED_PREFIX + email;
        return redisTemplate.hasKey(verifiedKey);
    }

    /**
     * Get remaining time until OTP expiration in seconds
     * 
     * @param email User email
     * @return remaining seconds, or 0 if expired
     */
    public long getRemainingExpirationTime(String email) {
        String key = OTP_PREFIX + email;
        Long expireTime = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expireTime != null && expireTime > 0 ? expireTime : 0;
    }
}
