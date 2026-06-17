package com.skyway.airline.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "password", "reservations" })
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "reservations" })
    private Flight flight;

    private String seatNumber;
    private String passengerName;
    private BigDecimal totalPrice;

    @Builder.Default
    private String status = "CONFIRMED";

    @Builder.Default
    private LocalDateTime bookedAt = LocalDateTime.now();

    // cancelledAt intentionally has NO default — it's null until cancelled
    private LocalDateTime cancelledAt;
}