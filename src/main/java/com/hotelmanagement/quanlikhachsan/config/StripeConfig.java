package com.hotelmanagement.quanlikhachsan.config;

import com.stripe.Stripe;
import com.stripe.exception.RateLimitException;
import com.stripe.exception.StripeException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.*;

@Component
@Slf4j
public class StripeConfig {
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${stripe.retry.initial-delay-ms:1000}")
    private int initialDelayMs;

    @Value("${stripe.retry.max-delay-ms:10000}")
    private int maxDelayMs;

    @Value("${stripe.retry.multiplier:2.0}")
    private double retryMultiplier;

    private final Random random = new Random();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("Stripe API initialized with retry configuration: maxAttempts={}, initialDelay={}ms",
                maxRetryAttempts, initialDelayMs);
    }

    public <T> T executeWithRetry(StripeOperation<T> operation) throws StripeException {
        int attempt = 0;
        int delay = initialDelayMs;
        StripeException lastException = null;

        while (attempt < maxRetryAttempts) {
            try {
                return operation.execute();
            } catch (RateLimitException e) {
                lastException = e;
                attempt++;
                log.warn("Stripe rate limit exceeded, attempt {}/{}, waiting {}ms before retry",
                        attempt, maxRetryAttempts, delay);
                if (attempt >= maxRetryAttempts) {
                    log.error("Max retry attempts exceeded for Stripe rate limit error");
                    throw lastException;
                }
                sleepWithJitter(delay);
                delay = (int) Math.min(delay * retryMultiplier, maxDelayMs);
            } catch (StripeException e) {
                String errorCode = e.getCode();
                if (isRetryableError(errorCode)) {
                    lastException = e;
                    attempt++;
                    log.warn("Stripe error {} ({}), attempt {}/{}, waiting {}ms before retry",
                            e.getMessage(), errorCode, attempt, maxRetryAttempts, delay);
                    if (attempt >= maxRetryAttempts) {
                        log.error("Max retry attempts exceeded for Stripe error: {}", errorCode);
                        throw lastException;
                    }
                    sleepWithJitter(delay);
                    delay = (int) Math.min(delay * retryMultiplier, maxDelayMs);
                } else {
                    log.error("Non-retryable Stripe error: {} ({})", e.getMessage(), errorCode);
                    throw e;
                }
            }
        }
        throw lastException;
    }

    private boolean isRetryableError(String errorCode) {
        if (errorCode == null) {
            return false;
        }
        return errorCode.equals("rate_limit") ||
                errorCode.equals("api_connection_error") ||
                errorCode.equals("temporary_service_error");
    }

    private void sleepWithJitter(int delayMs) {
        int jitter = (int) (delayMs * 0.1 * random.nextDouble());
        int actualDelay = delayMs + jitter;
        try {
            Thread.sleep(actualDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry delay interrupted");
        }
    }

    @FunctionalInterface
    public interface StripeOperation<T> {
        T execute() throws StripeException;
    }
}
