package com.skyway.airline.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "passengers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long passengerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "passengers", "user", "flight" })
    private Reservation reservation;

    private String passengerName;
    private String seatNumber;

    @Builder.Default
    private String status = "CONFIRMED";
}