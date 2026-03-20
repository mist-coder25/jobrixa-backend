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

    @Async
    public void sendWelcomeEmail(String to, String name) {
        try {
            Resend resend = new Resend(resendApiKey);
            CreateEmailOptions request = CreateEmailOptions.builder()
                .from("Jobrixa <onboarding@resend.dev>")
                .to(List.of(to))
                .subject("Welcome to Jobrixa! 🎯")
                .html("""
                    <div style="font-family:sans-serif;max-width:500px;margin:auto">
                    <h2>Welcome to Jobrixa, %s! 🎯</h2>
                    <p>Your job hunt is now organized. Here's how to get started:</p>
                    <ol>
                    <li>Add your first job application to the Pipeline</li>
                    <li>Track every stage — OA, Interview, Offer</li>
                    <li>Check your Analytics to see your response rate</li>
                    </ol>
                    <a href="https://jobrixa-frontend.vercel.app/pipeline" 
                       style="background:#4F8EF7;color:white;padding:12px 24px;border-radius:6px;text-decoration:none">
                       Go to my Pipeline →
                    </a>
                    <p style="color:#888;margin-top:24px">Free plan: 30 applications. Upgrade anytime at ₹149/month.</p>
                    </div>
                """.formatted(name))
                .build();
            resend.emails().send(request);
        } catch (Exception e) {
            System.err.println("Welcome email failed: " + e.getMessage());
        }
    }
}
