package com.hotelmanagement.quanlikhachsan.services.stripe;

import com.hotelmanagement.quanlikhachsan.dto.request.payment.CheckoutSessionRequestDTO;

import com.hotelmanagement.quanlikhachsan.dto.request.payment.PaymentRequestDTO;
import com.hotelmanagement.quanlikhachsan.dto.response.payment.CheckoutSessionResponseDTO;
import com.hotelmanagement.quanlikhachsan.exception.ErrorDefinition;
import com.hotelmanagement.quanlikhachsan.model.payment.Payment;
import com.hotelmanagement.quanlikhachsan.model.payment.PaymentStatus;
import com.hotelmanagement.quanlikhachsan.model.reservation.Reservation;
import com.hotelmanagement.quanlikhachsan.model.reservation.ReservationRoom;
import com.hotelmanagement.quanlikhachsan.model.reservation.ReservationStatus;
import com.hotelmanagement.quanlikhachsan.repository.PaymentRepository;
import com.hotelmanagement.quanlikhachsan.repository.PaymentStatusRepository;
import com.hotelmanagement.quanlikhachsan.repository.ReservationRepository;
import com.hotelmanagement.quanlikhachsan.services.email.EmailService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeServiceImpl implements IStripeService {
        @Value("${stripe.secret.key}")
        private String stripeSecretKey;

        @Value("${stripe.currency:usd}")
        private String currency;

        @Value("${stripe.max-amount:10000000}")
        private long maxAmountInCents;

        private final PaymentRepository paymentRepository;
        private final PaymentStatusRepository paymentStatusRepository;
        private final ReservationRepository reservationRepository;
        private final EmailService emailService;

        private PaymentStatus getPaymentStatus(String statusName) {
                return paymentStatusRepository.findByName(statusName)
                                .orElseThrow(() -> new RuntimeException("Payment status not found: " + statusName));
        }

        private void validateAmount(BigDecimal amount) {
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Payment amount must be positive");
                }
                long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
                if (amountInCents > maxAmountInCents) {
                        throw new IllegalArgumentException("Payment amount exceeds maximum allowed: " + maxAmountInCents / 100);
                }
        }

        @PostConstruct
        public void init() {
                Stripe.apiKey = stripeSecretKey;
        }

        @Override
        @Transactional
        public CheckoutSessionResponseDTO createCheckoutSession(CheckoutSessionRequestDTO request)
                        throws StripeException {
                // 1. Fetch reservation from database
                Reservation reservation = reservationRepository.findById(request.reservationId())
                                .orElseThrow(() -> ErrorDefinition.RESERVATION_NOT_FOUND
                                                .toAppError()
                                                .withDetail("Reservation not found", request.reservationId()));

                // 2. Calculate total amount
                long nights = ChronoUnit.DAYS.between(reservation.getCheckIn(), reservation.getCheckOut());
                if (nights <= 0)
                        nights = 1;

                BigDecimal totalAmount = reservation.getTotalAmount();
                validateAmount(totalAmount);
                long amountInCents = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();

                // 3. Build room description for line item
                String roomDescription = buildRoomDescription(reservation);

                // 4. Create Stripe Checkout Session
                SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                                .setMode(SessionCreateParams.Mode.PAYMENT)
                                .setSuccessUrl(request.successUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                                .setCancelUrl(request.cancelUrl())
                                .addLineItem(
                                                SessionCreateParams.LineItem.builder()
                                                                .setQuantity(1L)
                                                                .setPriceData(
                                                                                SessionCreateParams.LineItem.PriceData
                                                                                                .builder()
                                                                                                .setCurrency(currency)
                                                                                                .setUnitAmount(amountInCents)
                                                                                                .setProductData(
                                                                                                                SessionCreateParams.LineItem.PriceData.ProductData
                                                                                                                                .builder()
                                                                                                                                .setName("Đặt phòng khách sạn - "
                                                                                                                                                + nights
                                                                                                                                                + " đêm")
                                                                                                                                .setDescription(roomDescription)
                                                                                                                                .build())
                                                                                                .build())
                                                                .build())
                                .putMetadata("reservationId", request.reservationId().toString());

                Session session = Session.create(paramsBuilder.build());

                // 5. Create pending payment record
                PaymentStatus pendingStatus = paymentStatusRepository.findByName("PENDING")
                                .orElseThrow(() -> new RuntimeException("Payment status PENDING not found"));

                Payment payment = Payment.builder()
                                .reservation(reservation)
                                .amount(totalAmount)
                                .method("STRIPE_CHECKOUT")
                                .stripeSessionId(session.getId())
                                .status(pendingStatus)
                                .build();

                paymentRepository.save(payment);
                log.info("Created Stripe Checkout Session {} for reservation {}", session.getId(),
                                request.reservationId());

                return new CheckoutSessionResponseDTO(session.getId(), session.getUrl());
        }

        private String buildRoomDescription(Reservation reservation) {
                StringBuilder sb = new StringBuilder();
                sb.append("Check-in: ").append(reservation.getCheckIn());
                sb.append(" | Check-out: ").append(reservation.getCheckOut());

                if (reservation.getReservationRooms() != null && !reservation.getReservationRooms().isEmpty()) {
                        sb.append(" | Phòng: ");
                        for (ReservationRoom rr : reservation.getReservationRooms()) {
                                if (rr.getRoom() != null) {
                                        sb.append(rr.getRoom().getRoomNumber()).append(" ");
                                }
                        }
                }

                return sb.toString().trim();
        }

        @Transactional
        @Override
        public Payment processPayment(PaymentRequestDTO request) throws StripeException {
                Reservation reservation = reservationRepository.findById(request.reservationId())
                                .orElseThrow(() -> ErrorDefinition.RESERVATION_NOT_FOUND
                                                .toAppError()
                                                .withDetail("Reservation not found", request.reservationId()));

                // Create PaymentIntent on Stripe
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                                .setAmount(request.amount())
                                .setCurrency(currency)
                                .setPaymentMethod(request.token())
                                .setConfirm(true)
                                .setReturnUrl("http://localhost:3000/payment-success")
                                .setAutomaticPaymentMethods(
                                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                                                .setEnabled(true)
                                                                .setAllowRedirects(
                                                                                PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                                                .build())
                                .build();

                PaymentIntent paymentIntent = PaymentIntent.create(params);

                // Determine status based on Stripe result
                String statusName;
                if ("succeeded".equals(paymentIntent.getStatus())) {
                        statusName = "COMPLETED";
                        reservation.setStatus(ReservationStatus.CONFIRMED);
                        reservationRepository.save(reservation);
                        log.info("Payment successful for reservation {}, Stripe status: {}", reservation.getId(), paymentIntent.getStatus(),
                                        reservation.getId());
                } else if ("requires_payment_method".equals(paymentIntent.getStatus())) {
                        log.warn("Payment failed for reservation {}, Stripe status: {}", reservation.getId(), paymentIntent.getStatus());
                        statusName = "FAILED";
                } else {
                    statusName = "PENDING";
                }

            PaymentStatus status = paymentStatusRepository.findByName(statusName)
                                .orElseThrow(() -> new RuntimeException("Status not found in DB: " + statusName));

                // Save payment record
                Payment payment = Payment.builder()
                                .reservation(reservation)
                                .amount(BigDecimal.valueOf(request.amount() / 100.0))
                                .method("STRIPE")
                                .transactionCode(paymentIntent.getId())
                                .status(status)
                                .build();

                return paymentRepository.save(payment);
        }

        @Override
        @Transactional
        public void updatePaymentStatusFromWebhook(String paymentIntentId, String statusName) {
                Payment payment = paymentRepository.findByTransactionCode(paymentIntentId)
                                .orElse(null);

                if (payment != null) {
                        PaymentStatus newStatus = paymentStatusRepository.findByName(statusName)
                                        .orElseThrow(() -> new RuntimeException("Status not found"));

                        payment.setStatus(newStatus);
                        paymentRepository.save(payment);
                        log.info("Payment status updated for PaymentIntent {}", paymentIntentId);
                }
        }

        @Override
        @Transactional
        public void handleCheckoutSessionCompleted(String sessionId) {
                Payment payment = paymentRepository.findByStripeSessionId(sessionId)
                                .orElse(null);

                if (payment != null) {
                        // Update payment status to COMPLETED
                        PaymentStatus completedStatus = paymentStatusRepository.findByName("COMPLETED")
                                        .orElseThrow(() -> new RuntimeException("Status COMPLETED not found"));
                        payment.setStatus(completedStatus);
                        paymentRepository.save(payment);

                        // Update reservation status to CONFIRMED
                        Reservation reservation = payment.getReservation();
                        if (reservation != null && reservation.getStatus() == ReservationStatus.PENDING) {
                                reservation.setStatus(ReservationStatus.CONFIRMED);
                                reservationRepository.save(reservation);
                                log.info("Reservation {} status updated to CONFIRMED after Checkout Session completed",
                                                reservation.getId());

                                // Send booking confirmation email
                                sendConfirmationEmail(reservation, payment);
                        }

                        log.info("Checkout Session {} completed, payment and reservation updated", sessionId);
                } else {
                        log.warn("No payment found for Checkout Session {}", sessionId);
                }
        }

        /**
         * Send booking confirmation email to guest
         */
        private void sendConfirmationEmail(Reservation reservation, Payment payment) {
                try {
                        // Get guest email from reservation
                        String guestEmail = reservation.getGuest().getEmail();
                        String guestName = reservation.getGuest().getFullName();
                        String reservationId = reservation.getId().toString();

                        // Build room info
                        StringBuilder roomInfo = new StringBuilder();
                        if (reservation.getReservationRooms() != null && !reservation.getReservationRooms().isEmpty()) {
                                for (ReservationRoom rr : reservation.getReservationRooms()) {
                                        if (rr.getRoom() != null) {
                                                if (!roomInfo.isEmpty())
                                                        roomInfo.append(", ");
                                                roomInfo.append("Phòng ").append(rr.getRoom().getRoomNumber());
                                                if (rr.getRoom().getType().getName() != null) {
                                                        roomInfo.append(" (")
                                                                        .append(rr.getRoom().getType().getName())
                                                                        .append(")");
                                                }
                                        }
                                }
                        }
                        if (!roomInfo.toString().isEmpty()) {
                                roomInfo.append("N/A");
                        }

                        // Format dates
                        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        String checkIn = reservation.getCheckIn().format(dateFormatter);
                        String checkOut = reservation.getCheckOut().format(dateFormatter);

                        // Format amount
                        String totalAmount = "$" + payment.getAmount().toString();

                        // Send email
                        emailService.sendBookingConfirmationEmail(
                                        guestEmail,
                                        guestName,
                                        reservationId,
                                        roomInfo.toString(),
                                        checkIn,
                                        checkOut,
                                        totalAmount);

                        log.info("Booking confirmation email sent for reservation {}", reservationId);
                } catch (Exception e) {
                        log.error("Failed to send booking confirmation email for reservation {}",
                                        reservation.getId(), e);
                        // Don't throw - email failure shouldn't break the payment flow
                }
        }
}
