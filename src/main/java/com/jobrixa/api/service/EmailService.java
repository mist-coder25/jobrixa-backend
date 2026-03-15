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
    public void sendWelcome(String to, String name) {
        try {
            Resend resend = new Resend(resendApiKey);
            CreateEmailOptions request = CreateEmailOptions.builder()
                .from("Jobrixa <onboarding@resend.dev>")
                .to(List.of(to))
                .subject("Welcome to Jobrixa! 🎯")
                .html("<h2>Hey " + name + ", welcome to Jobrixa!</h2>" +
                      "<p>You're all set to start tracking your job applications like a pro.</p>" +
                      "<p><strong>Here's how to get started:</strong></p>" +
                      "<ol>" +
                      "<li>Add your first job application to the Pipeline</li>" +
                      "<li>Check your Dashboard for real-time analytics</li>" +
                      "<li>Use Discover to find trusted job listings</li>" +
                      "</ol>" +
                      "<p>Free plan includes 30 applications. Need more? <a href='https://jobrixa-frontend.vercel.app/pricing'>Upgrade to Pro</a> anytime.</p>" +
                      "<p>Good luck with your job hunt! 🚀</p>" +
                      "<p>— The Jobrixa Team</p>")
                .build();
            resend.emails().send(request);
            System.out.println("Welcome email sent to: " + to);
        } catch (Exception e) {
            System.err.println("Welcome email failed: " + e.getMessage());
        }
    }
}
