package com.jobrixa.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendOtp(String to, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Your Jobrixa OTP");
        msg.setText("Your OTP is: " + otp + "\n\nValid for 10 minutes.\n\n- Jobrixa Team");
        msg.setFrom("snehalthube29@gmail.com");
        mailSender.send(msg);
    }
}
