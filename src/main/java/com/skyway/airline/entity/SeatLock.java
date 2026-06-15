package com.skyway.airline.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seat_locks", uniqueConstraints = @UniqueConstraint(columnNames = { "flight_id", "seat_number" }))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_id")
    private Long flightId;

    private String seatNumber;

    @Column(name = "user_id")
    private Long userId;

    // Fix: @Builder.Default on both timestamp fields
    @Builder.Default
    private LocalDateTime lockedAt = LocalDateTime.now();

    // expiresAt is set explicitly in SeatLockService, no default needed
    private LocalDateTime expiresAt;
}