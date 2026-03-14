package com.jobrixa.api.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {
    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Async
    public void sendOtp(String to, String otp) {
        try {
            Resend resend = new Resend(resendApiKey);
            CreateEmailOptions options = CreateEmailOptions.builder()
                .from("Jobrixa <onboarding@resend.dev>")
                .to(List.of(to))
                .subject("Your Jobrixa OTP")
                .html("<p>Your OTP is: <strong>" + otp + "</strong></p><p>Valid for 10 minutes.</p>")
                .build();
            resend.emails().send(options);
            System.out.println("OTP sent via Resend to: " + to);
        } catch (Exception e) {
            System.err.println("Resend failed: " + e.getMessage());
        }
    }
}
