package com.jobrixa.api.controller;

import com.jobrixa.api.entity.Payment;
import com.jobrixa.api.entity.User;
import com.jobrixa.api.service.PaymentService;
import com.jobrixa.api.service.PlanLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@CrossOrigin
public class PaymentController {

    private final PaymentService paymentService;
    private final PlanLimitService planLimitService;

    /** Creates a Razorpay order and returns orderId + amount for frontend checkout. */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        System.out.println("Payment Create Order Request: " + body);
        try {
            User user = (User) userDetails;
            String plan = body.getOrDefault("plan", "PRO").toUpperCase();
            Integer amount = null;
            if (body.containsKey("amount")) {
                try {
                    amount = Integer.parseInt(body.get("amount"));
                } catch (NumberFormatException e) {
                    // Ignore or log
                }
            }
            Map<String, Object> result = paymentService.createOrder(plan, amount, user);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Verifies Razorpay signature and upgrades user plan on success. */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        User user = (User) userDetails;
        String orderId = body.get("razorpay_order_id");
        String paymentId = body.get("razorpay_payment_id");
        String signature = body.get("razorpay_signature");

        String updatedPlan = paymentService.verifyPayment(orderId, paymentId, signature, user);
        if (updatedPlan != null) {
            return ResponseEntity.ok(Map.of("success", true, "plan", updatedPlan));
        }
        return ResponseEntity.status(400).body(Map.of("success", false, "error", "Payment verification failed"));
    }

    /** Returns current user's plan, expiry, and payment history. */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        User user = (User) userDetails;
        List<Payment> history = paymentService.getPaymentHistory(user);
        boolean active = planLimitService.isPlanActive(user);

        return ResponseEntity.ok(Map.of(
            "plan", user.getPlan(),
            "planExpiresAt", user.getPlanExpiresAt() != null ? user.getPlanExpiresAt().toString() : null,
            "isActive", active,
            "applicationCount", user.getPlan(),  // placeholder — overridden FE side
            "payments", history.stream().map(p -> Map.of(
                "id", p.getId(),
                "plan", p.getPlan(),
                "amount", p.getAmount(),
                "status", p.getStatus(),
                "createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : ""
            )).toList()
        ));
    }
}
