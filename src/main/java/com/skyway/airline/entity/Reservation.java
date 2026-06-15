package com.skyway.airline.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    @Column(unique = true, nullable = false)
    private String pnr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    private Flight flight;

    private String seatNumber;
    private String passengerName;
    private BigDecimal totalPrice;

    // Fix: @Builder.Default required whenever a field has an initializer
    // Without it, @Builder ignores "CONFIRMED" and sets null
    @Builder.Default
    private String status = "CONFIRMED";

    @Builder.Default
    private LocalDateTime bookedAt = LocalDateTime.now();

    // cancelledAt intentionally has NO default — it's null until cancelled
    private LocalDateTime cancelledAt;
}