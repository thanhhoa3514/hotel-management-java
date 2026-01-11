package com.hotelmanagement.quanlikhachsan.repository;

import com.hotelmanagement.quanlikhachsan.model.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentStatusRepository extends JpaRepository<PaymentStatus, UUID> {
    Optional<PaymentStatus> findByName(String name);
}
