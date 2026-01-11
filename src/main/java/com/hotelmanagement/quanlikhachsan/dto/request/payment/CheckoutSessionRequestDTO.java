package com.hotelmanagement.quanlikhachsan.dto.request.payment;

import java.util.UUID;

public record CheckoutSessionRequestDTO(
        UUID reservationId,
        String successUrl,
        String cancelUrl) {
}
