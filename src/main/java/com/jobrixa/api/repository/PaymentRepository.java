package com.jobrixa.api.repository;

import com.jobrixa.api.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}
