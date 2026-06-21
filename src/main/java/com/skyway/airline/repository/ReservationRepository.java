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

        // Required by AdminReservationController.getAll()
        List<Reservation> findAllByOrderByBookedAtDesc();

        // Required by AdminReservationController.search()
        // All filters are optional — pass null for any filter you don't want applied.
        @Query("SELECT DISTINCT r FROM Reservation r " +
                        "LEFT JOIN r.passengers p " +
                        "WHERE (:pnr IS NULL OR r.pnr = :pnr) " +
                        "AND (:passengerName IS NULL OR LOWER(p.passengerName) LIKE LOWER(CONCAT('%', :passengerName, '%'))) "
                        +
                        "AND (:flightNumber IS NULL OR r.flight.flightNumber = :flightNumber) " +
                        "AND (:email IS NULL OR r.user.email = :email) " +
                        "AND (:bookingDate IS NULL OR FUNCTION('DATE', r.bookedAt) = :bookingDate) " +
                        "ORDER BY r.bookedAt DESC")
        List<Reservation> adminSearch(
                        @Param("pnr") String pnr,
                        @Param("passengerName") String passengerName,
                        @Param("flightNumber") String flightNumber,
                        @Param("email") String email,
                        @Param("bookingDate") LocalDate bookingDate);
}