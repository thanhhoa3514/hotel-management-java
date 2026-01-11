package com.hotelmanagement.quanlikhachsan.repository;

import com.hotelmanagement.quanlikhachsan.model.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByTransactionCode(String transactionCode);

    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    Optional<Payment> findByReservationId(UUID reservationId);
}
