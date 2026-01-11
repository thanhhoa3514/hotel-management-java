package com.hotelmanagement.quanlikhachsan.controller;

import com.hotelmanagement.quanlikhachsan.dto.request.payment.CheckoutSessionRequestDTO;
import com.hotelmanagement.quanlikhachsan.dto.request.payment.PaymentRequestDTO;
import com.hotelmanagement.quanlikhachsan.services.stripe.IStripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" })
@Slf4j
public class PaymentController {
    private final IStripeService stripeService;
    private final StringRedisTemplate redisTemplate;

    @Value("${stripe.rate-limit.attempts:5}")
    private int rateLimitAttempts;

    @Value("${stripe.rate-limit.window-seconds:60}")
    private int rateLimitWindowSeconds;

    private boolean isRateLimited(String ipAddress) {
        String key = "stripe:rate:" + ipAddress;
        String countStr = redisTemplate.opsForValue().get(key);

        if (countStr == null) {
            redisTemplate.opsForValue().set(key, "1", rateLimitWindowSeconds, TimeUnit.SECONDS);
            return false;
        }

        int count = Integer.parseInt(countStr);
        if (count >= rateLimitAttempts) {
            log.warn("Rate limit exceeded for IP: {}", ipAddress);
            return true;
        }

        redisTemplate.opsForValue().increment(key);
        return false;
    }

    /**
     * Process direct payment with PaymentIntent (legacy method)
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody PaymentRequestDTO paymentRequest,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {
        String ip = clientIp != null ? clientIp.split(",")[0].trim() : "unknown";

        if (isRateLimited(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Please try again later.");
        }

        try {
            var payment = stripeService.processPayment(paymentRequest);
            return ResponseEntity.ok(payment);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error for payment from IP {}: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Payment error from IP {}: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Create Stripe Checkout Session for hosted payment page
     * This endpoint creates a session and returns the URL to redirect the user to
     * Stripe
     */
    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody CheckoutSessionRequestDTO request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {
        String ip = clientIp != null ? clientIp.split(",")[0].trim() : "unknown";

        if (isRateLimited(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Please try again later.");
        }

        try {
            var response = stripeService.createCheckoutSession(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error for checkout session from IP {}: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Checkout session error from IP {}: {}", ip, e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
