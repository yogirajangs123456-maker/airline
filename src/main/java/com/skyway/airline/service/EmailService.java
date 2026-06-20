package com.skyway.airline.service;

import com.skyway.airline.entity.Passenger;
import com.skyway.airline.entity.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public void sendBookingConfirmation(Reservation reservation) {
        String passengerList = reservation.getPassengers().stream()
                .map(p -> p.getPassengerName() + " — Seat " + p.getSeatNumber())
                .collect(Collectors.joining("<br/>"));

        String html = String.format(
                "<p>Dear %s,</p>" +
                        "<p>Your booking is confirmed!</p>" +
                        "<p><b>PNR:</b> %s</p>" +
                        "<p><b>Flight:</b> %s %s</p>" +
                        "<p><b>Route:</b> %s → %s</p>" +
                        "<p><b>Departure:</b> %s &nbsp; <b>Arrival:</b> %s</p>" +
                        "<p><b>Passengers:</b><br/>%s</p>" +
                        "<p><b>Total Paid:</b> ₹%s</p>" +
                        "<p>You can download your ticket anytime from: My Bookings → Download Ticket</p>" +
                        "<p>— SkyWay Airlines Team</p>",
                reservation.getUser().getName(),
                reservation.getPnr(),
                reservation.getFlight().getAirlineCode(), reservation.getFlight().getFlightNumber(),
                reservation.getFlight().getSource(), reservation.getFlight().getDestination(),
                reservation.getFlight().getDepartureTime().format(TIME_FMT),
                reservation.getFlight().getArrivalTime().format(TIME_FMT),
                passengerList,
                reservation.getTotalPrice().toPlainString());

        send(reservation.getUser().getEmail(), "SkyWay Airlines — Booking Confirmed: " + reservation.getPnr(), html);
    }

    public void sendCancellationConfirmation(Reservation reservation, List<Passenger> cancelledPassengers,
            BigDecimal refundAmount) {
        String cancelledList = cancelledPassengers.stream()
                .map(Passenger::getPassengerName)
                .collect(Collectors.joining("<br/>"));

        String remainingList = reservation.getPassengers().stream()
                .filter(p -> "CONFIRMED".equals(p.getStatus()))
                .map(Passenger::getPassengerName)
                .collect(Collectors.joining("<br/>"));

        if (remainingList.isEmpty())
            remainingList = "None";

        String html = String.format(
                "<p>Dear %s,</p>" +
                        "<p><b>PNR:</b> %s</p>" +
                        "<p><b>Cancelled:</b><br/>%s</p>" +
                        "<p><b>Refund Amount:</b> ₹%s</p>" +
                        "<p><b>Remaining Active Passengers:</b><br/>%s</p>" +
                        "<p><b>Booking Status:</b> %s</p>" +
                        "<p>— SkyWay Airlines Team</p>",
                reservation.getUser().getName(),
                reservation.getPnr(),
                cancelledList,
                refundAmount.toPlainString(),
                remainingList,
                reservation.getStatus());

        send(reservation.getUser().getEmail(), "SkyWay Airlines — Cancellation Confirmed: " + reservation.getPnr(),
                html);
    }

    private void send(String toEmail, String subject, String htmlContent) {
        WebClient client = WebClient.create("https://api.brevo.com");

        Map<String, Object> sender = Map.of("name", "SkyWay Airlines", "email", senderEmail);
        Map<String, Object> to = Map.of("email", toEmail);
        Map<String, Object> body = Map.of(
                "sender", sender,
                "to", new Object[] { to },
                "subject", subject,
                "htmlContent", htmlContent);

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
            System.out.println("EMAIL SENT TO: " + toEmail + " | Subject: " + subject);
        } catch (Exception e) {
            // Don't let email failure break the booking/cancellation flow itself
            System.out.println("EMAIL FAILED: " + e.getMessage());
        }
    }
}