package com.jobrixa.api.service;

import com.jobrixa.api.entity.Payment;
import com.jobrixa.api.entity.User;
import com.jobrixa.api.repository.PaymentRepository;
import com.jobrixa.api.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    @Value("${razorpay.key-id:#{null}}")
    private String keyId;

    @Value("${razorpay.key-secret:#{null}}")
    private String keySecret;

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @PostConstruct
    public void init() {
        if (keyId != null && keyId.length() >= 8) {
            System.out.println("Razorpay Key ID loaded: " + keyId.substring(0, 8) + "...");
        } else {
            System.err.println("Razorpay Key ID is NULL or too short!");
        }
    }

    public static final Map<String, Integer> PLAN_PRICES = Map.of(
        "PRO_MONTHLY", 14900,
        "PRO_YEARLY", 99900,
        "CAMPUS", 49900
    );

    /**
     * Creates a Razorpay order and persists a PENDING payment record.
     */
    @Transactional
    public Map<String, Object> createOrder(String plan, Integer requestedAmount, User user) throws RazorpayException {
        Integer amount = requestedAmount != null ? requestedAmount : PLAN_PRICES.get(plan.toUpperCase());
        if (amount == null) {
            throw new IllegalArgumentException("Unknown plan or missing amount");
        }

        if (keyId == null || keySecret == null) {
            // Test mode — return mock data
            String mockOrderId = "order_mock_" + System.currentTimeMillis();
            persistPayment(user, mockOrderId, plan, amount);
            return Map.of("orderId", mockOrderId, "amount", amount, "currency", "INR", "key", "rzp_test_mock");
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "jbx_" + System.currentTimeMillis());
            orderRequest.put("payment_capture", 1);

            Order order = client.orders.create(orderRequest);
            String orderId = order.get("id");

            persistPayment(user, orderId, plan, amount);

            return Map.of("orderId", orderId, "amount", amount, "currency", "INR", "key", keyId);
        } catch (RazorpayException e) {
            System.err.println("Razorpay Order Creation Failed!");
            System.err.println("Error Message: " + e.getMessage());
            System.err.println("Razorpay Trace: " + e.getStackTrace());
            throw e;
        }
    }

    private void persistPayment(User user, String orderId, String plan, int amount) {
        paymentRepository.save(Payment.builder()
            .user(user)
            .razorpayOrderId(orderId)
            .plan(plan.toUpperCase())
            .amount(amount)
            .status("PENDING")
            .build());
    }

    /**
     * Verifies Razorpay HMAC-SHA256 signature, then upgrades the user's plan.
     */
    @Transactional
    public boolean verifyPayment(String orderId, String paymentId, String signature, User user) {
        try {
            if (keySecret != null && !keySecret.isBlank()) {
                String payload = orderId + "|" + paymentId;
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                String generated = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
                if (!generated.equals(signature)) {
                    return false;
                }
            }

            // Find the payment record
            Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

            payment.setStatus("SUCCESS");
            payment.setRazorpayPaymentId(paymentId);
            paymentRepository.save(payment);

            // Re-fetch user to ensure it's attached to session
            User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Upgrade user plan
            String plan = payment.getPlan();
            currentUser.setPlan("PRO_YEARLY".equals(plan) || "PRO_MONTHLY".equals(plan) ? "PRO" : plan);
            
            LocalDateTime expiry;
            if ("PRO_YEARLY".equals(plan)) {
                expiry = LocalDateTime.now().plusYears(1);
            } else if ("CAMPUS".equals(plan)) {
                expiry = LocalDateTime.now().plusMonths(6);
            } else {
                expiry = LocalDateTime.now().plusMonths(1);
            }
            currentUser.setPlanExpiresAt(expiry);
            userRepository.save(currentUser);
            System.out.println("User plan upgraded to: " + currentUser.getPlan() + " for " + currentUser.getEmail());

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<Payment> getPaymentHistory(User user) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}
