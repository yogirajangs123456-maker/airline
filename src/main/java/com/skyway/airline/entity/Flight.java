package com.skyway.airline.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "flights")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long flightId;

    private String airlineCode;
    private String airlineName;
    private String flightNumber;
    private String source;
    private String destination;

    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private LocalDate journeyDate;

    private BigDecimal price;
    private int totalSeats;
    private int availableSeats;
    private String duration;

    @Builder.Default
    private boolean active = true;

    // NEW — nullable; null for manually created flights, populated for
    // scheduler-generated ones
    private Long templateId;

    /**
     * NEW — computed, not stored in DB.
     * CANCELLED maps directly to active=false (no new column, no breaking change).
     */
    @Transient
    public String getStatus() {
        if (!active) {
            return "CANCELLED";
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departureDateTime = LocalDateTime.of(journeyDate, departureTime);
        LocalDateTime arrivalDateTime = LocalDateTime.of(journeyDate, arrivalTime);

        if (now.isBefore(departureDateTime)) {
            return "SCHEDULED";
        } else if (now.isBefore(arrivalDateTime)) {
            return "DEPARTED";
        } else {
            return "COMPLETED";
        }
    }

    @Transient
    public boolean isBookable() {
        return "SCHEDULED".equals(getStatus());
    }
}