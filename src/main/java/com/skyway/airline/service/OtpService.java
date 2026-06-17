package com.skyway.airline.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OtpService {

    @Value("${app.otp.expiry:10}")
    private int otpExpiryMinutes;

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    public void generateAndSendOTP(String email, String pnr) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        otpStore.put(email, new OtpEntry(otp, pnr, expiry));

        sendEmailViaBrevo(email, otp, pnr);
    }

    private void sendEmailViaBrevo(String toEmail, String otp, String pnr) {
        WebClient client = WebClient.create("https://api.brevo.com");

        Map<String, Object> sender = Map.of(
                "name", "SkyWay Airlines",
                "email", senderEmail);

        Map<String, Object> to = Map.of("email", toEmail);

        Map<String, Object> body = Map.of(
                "sender", sender,
                "to", new Object[] { to },
                "subject", "SkyWay Airlines — Cancellation OTP",
                "htmlContent", String.format(
                        "<p>Dear Passenger,</p>" +
                                "<p>Your OTP for cancelling ticket PNR <b>%s</b> is: <b>%s</b></p>" +
                                "<p>This OTP is valid for %d minutes.</p>" +
                                "<p>If you did not request this, please ignore.</p>" +
                                "<p>— SkyWay Airlines Team</p>",
                        pnr, otp, otpExpiryMinutes));

        try {
            client.post()
                    .uri("/v3/smtp/email")
                    .header("api-key", brevoApiKey)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            System.out.println("OTP EMAIL SENT VIA BREVO TO: " + toEmail);
        } catch (Exception e) {
            System.out.println("BREVO EMAIL FAILED: " + e.getMessage());
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
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

    @Scheduled(fixedDelay = 60_000)
    public void cleanExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        otpStore.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }

    record OtpEntry(String otp, String pnr, LocalDateTime expiresAt) {
    }
}