package com.skyway.airline.repository;

import com.skyway.airline.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

        Optional<Reservation> findByPnr(String pnr);

        List<Reservation> findByUser_Email(String email);

        @Query("SELECT COUNT(r) FROM Reservation r WHERE r.bookedAt >= :start AND r.bookedAt < :end")
        long countBookingsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        // ── Admin search — every filter optional ──
        @Query("SELECT DISTINCT r FROM Reservation r " +
                        "LEFT JOIN r.passengers p " +
                        "WHERE (:pnr IS NULL OR LOWER(r.pnr) LIKE LOWER(CONCAT('%', :pnr, '%'))) AND " +
                        "(:passengerName IS NULL OR LOWER(p.passengerName) LIKE LOWER(CONCAT('%', :passengerName, '%'))) AND "
                        +
                        "(:flightNumber IS NULL OR LOWER(r.flight.flightNumber) LIKE LOWER(CONCAT('%', :flightNumber, '%'))) AND "
                        +
                        "(:email IS NULL OR LOWER(r.user.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
                        "(:bookingDate IS NULL OR CAST(r.bookedAt AS date) = :bookingDate) " +
                        "ORDER BY r.bookedAt DESC")
        List<Reservation> adminSearch(
                        @Param("pnr") String pnr,
                        @Param("passengerName") String passengerName,
                        @Param("flightNumber") String flightNumber,
                        @Param("email") String email,
                        @Param("bookingDate") LocalDate bookingDate);

        List<Reservation> findAllByOrderByBookedAtDesc();
}