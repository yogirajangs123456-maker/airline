package com.skyway.airline.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "reservation" })
    @Builder.Default
    private List<Passenger> passengers = new ArrayList<>();

    private BigDecimal totalPrice;

    // Total refunded so far across all cancellations on this PNR
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Builder.Default
    private LocalDateTime bookedAt = LocalDateTime.now();

    private LocalDateTime cancelledAt;

    /**
     * Computed PNR-level status — NOT a stored column.
     * CONFIRMED if at least one passenger is still CONFIRMED.
     * CANCELLED only when every passenger is CANCELLED.
     */
    @Transient
    public String getStatus() {
        boolean anyConfirmed = passengers.stream()
                .anyMatch(p -> "CONFIRMED".equals(p.getStatus()));
        return anyConfirmed ? "ACTIVE" : "CANCELLED";
    }
}