package com.hotelmanagement.quanlikhachsan.controller;

import com.hotelmanagement.quanlikhachsan.model.payment.Payment;
import com.hotelmanagement.quanlikhachsan.model.payment.PaymentStatus;
import com.hotelmanagement.quanlikhachsan.repository.PaymentRepository;
import com.hotelmanagement.quanlikhachsan.repository.PaymentStatusRepository;
import com.hotelmanagement.quanlikhachsan.services.stripe.IStripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/webhook")
@Slf4j
public class StripeWebhookController {
    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @Value("${stripe.webhook.idempotency-ttl:86400}")
    private long idempotencyTtl;

    @Autowired
    private IStripeService stripeService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentStatusRepository paymentStatusRepository;

    private boolean isEventProcessed(String eventId) {
        String key = "stripe:webhook:" + eventId;
        Boolean exists = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            log.info("Duplicate webhook event detected: {}", eventId);
            return true;
        }
        redisTemplate.opsForValue().set(key, "1", idempotencyTtl, TimeUnit.SECONDS);
        return false;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event = null;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        String eventId = event.getId();
        if (isEventProcessed(eventId)) {
            log.info("Webhook event {} already processed, skipping", eventId);
            return ResponseEntity.ok("Duplicate event, already processed");
        }

        log.info("Received Stripe webhook event: {} with id: {}", event.getType(), eventId);

        // Handle different event types
        switch (event.getType()) {
            case "checkout.session.completed":
                try {
                    Session session = (Session) event.getDataObjectDeserializer()
                            .getObject()
                            .orElse(null);
                    if (session != null) {
                        log.info("Checkout Session completed: {}", session.getId());
                        stripeService.handleCheckoutSessionCompleted(session.getId());
                    } else {
                        log.warn("Session object was null in checkout.session.completed event");
                    }
                } catch (Exception e) {
                    log.error("Failed to deserialize checkout session: {}", e.getMessage());
                }
                break;

            case "payment_intent.succeeded":
                // Handle successful PaymentIntent (legacy flow)
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject()
                        .orElse(null);
                if (paymentIntent != null) {
                    log.info("PaymentIntent succeeded: {}", paymentIntent.getId());
                    stripeService.updatePaymentStatusFromWebhook(paymentIntent.getId(), "COMPLETED");
                }
                break;

            case "payment_intent.payment_failed":
                // Handle failed PaymentIntent
                PaymentIntent failedIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                if (failedIntent != null) {
                    log.warn("PaymentIntent failed: {}", failedIntent.getId());
                    stripeService.updatePaymentStatusFromWebhook(failedIntent.getId(), "FAILED");
                }
                break;

            case "checkout.session.expired":
                Session expiredSession = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (expiredSession != null) {
                    log.warn("Checkout Session expired: {}", expiredSession.getId());
                    paymentRepository.findByStripeSessionId(expiredSession.getId())
                            .ifPresent(payment -> {
                                if ("PENDING".equals(payment.getStatus().getName())) {
                                    PaymentStatus failedStatus = paymentStatusRepository.findByName("FAILED")
                                            .orElseThrow(() -> new RuntimeException("Status FAILED not found"));
                                    payment.setStatus(failedStatus);
                                    paymentRepository.save(payment);
                                    log.info("Payment {} marked as FAILED due to session expiry", payment.getId());
                                }
                            });
                }
                break;

            default:
                log.info("Unhandled event type: {}", event.getType());
        }

        // Always return 200 OK to acknowledge receipt
        return ResponseEntity.ok("Received");
    }
}
