package com.skyway.airline.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final JavaMailSender mailSender;

    @Value("${app.otp.expiry:10}")
    private int otpExpiryMinutes;

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    public void generateAndSendOTP(String email, String pnr) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        otpStore.put(email, new OtpEntry(otp, pnr, expiry));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("SkyWay Airlines — Cancellation OTP");
        message.setText(String.format(
                "Dear Passenger,\n\nYour OTP for cancelling ticket PNR %s is: %s\n\n" +
                        "This OTP is valid for %d minutes.\n\nIf you did not request this, please ignore.\n\n" +
                        "— SkyWay Airlines Team",
                pnr, otp, otpExpiryMinutes));
        mailSender.send(message);
    }

    public boolean verifyOTP(String email, String otp) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null)
            return false;
        if (entry.expiresAt().isBefore(LocalDateTime.now())) {
            otpStore.remove(email);
            return false;
        }
        boolean valid = entry.otp().equals(otp);
        if (valid)
            otpStore.remove(email);
        return valid;
    }

    // Bug #8: map was never cleaned — expired entries leaked indefinitely
    @Scheduled(fixedDelay = 60_000)
    public void cleanExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        otpStore.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }

    record OtpEntry(String otp, String pnr, LocalDateTime expiresAt) {
    }
}