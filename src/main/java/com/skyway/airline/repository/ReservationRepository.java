package com.skyway.airline.repository;

import com.skyway.airline.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

        Optional<Reservation> findByPnr(String pnr);

        List<Reservation> findByUser_Email(String email);

        @Query("SELECT COUNT(r) FROM Reservation r WHERE r.bookedAt >= :start AND r.bookedAt < :end")
        long countBookingsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}