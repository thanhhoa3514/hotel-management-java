package com.hotelmanagement.quanlikhachsan.services.stripe;

import com.hotelmanagement.quanlikhachsan.dto.request.payment.CheckoutSessionRequestDTO;
import com.hotelmanagement.quanlikhachsan.dto.request.payment.PaymentRequestDTO;
import com.hotelmanagement.quanlikhachsan.dto.response.payment.CheckoutSessionResponseDTO;
import com.hotelmanagement.quanlikhachsan.model.payment.Payment;
import com.stripe.exception.StripeException;

public interface IStripeService {
    Payment processPayment(PaymentRequestDTO request) throws StripeException;

    CheckoutSessionResponseDTO createCheckoutSession(CheckoutSessionRequestDTO request) throws StripeException;

    void updatePaymentStatusFromWebhook(String paymentIntentId, String statusName);

    void handleCheckoutSessionCompleted(String sessionId);
}
