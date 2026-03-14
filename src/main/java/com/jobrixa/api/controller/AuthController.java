package com.jobrixa.api.controller;

import com.jobrixa.api.dto.AuthResponse;
import com.jobrixa.api.dto.LoginRequest;
import com.jobrixa.api.dto.RegisterRequest;
import com.jobrixa.api.service.AuthService;
import com.jobrixa.api.repository.UserRepository;
import com.jobrixa.api.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.jobrixa.api.service.EmailService emailService;

    private final ConcurrentHashMap<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> resetTokenStore = new ConcurrentHashMap<>();
    record OtpEntry(String otp, LocalDateTime expiry) {}

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStore.put(email, new OtpEntry(otp, LocalDateTime.now().plusMinutes(15)));
        
        try {
            emailService.sendOtp(email, otp);
            log.info("OTP sent to {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to send email"));
        }
        
        return ResponseEntity.ok(Map.of("message", "OTP sent to your email"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        OtpEntry entry = otpStore.get(email);
        if (entry == null || !entry.otp().equals(otp) || LocalDateTime.now().isAfter(entry.expiry())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired OTP"));
        }
        String resetToken = UUID.randomUUID().toString();
        resetTokenStore.put(resetToken, email);
        otpStore.remove(email);
        return ResponseEntity.ok(Map.of("resetToken", resetToken));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("resetToken");
        String newPassword = body.get("newPassword");
        String email = resetTokenStore.get(token);
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid reset token"));
        }
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetTokenStore.remove(token);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
