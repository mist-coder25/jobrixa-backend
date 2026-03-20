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
                .html("<h2>Hey " + name + "!</h2>" +
                      "<p>Welcome to Jobrixa — your job hunt is now organized.</p>" +
                      "<p>Here's how to get started:</p>" +
                      "<ol>" +
                      "<li>Add your first job application to the Pipeline</li>" +
                      "<li>Set deadlines so you never miss an OA</li>" +
                      "<li>Check your Analytics to track your response rate</li>" +
                      "</ol>" +
                      "<p><a href='https://jobrixa-frontend.vercel.app/pipeline'>Go to your Pipeline →</a></p>" +
                      "<p>— The Jobrixa Team</p>")
                .build();
            resend.emails().send(request);
        } catch (Exception e) {
            System.err.println("Welcome email failed: " + e.getMessage());
        }
    }
}
