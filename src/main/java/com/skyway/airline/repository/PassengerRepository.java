package com.skyway.airline.repository;

import com.skyway.airline.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    @Query("SELECT p.seatNumber FROM Passenger p " +
            "WHERE p.reservation.flight.flightId = :flightId AND p.status = 'CONFIRMED'")
    List<String> findBookedSeatsByFlightId(@Param("flightId") Long flightId);

    List<Passenger> findByReservation_ReservationId(Long reservationId);

    // Count of DISTINCT reservations that have at least one CONFIRMED passenger
    @Query("SELECT COUNT(DISTINCT p.reservation.reservationId) FROM Passenger p WHERE p.status = 'CONFIRMED'")
    long countActiveReservations();

    // Count of DISTINCT reservations where EVERY passenger is CANCELLED
    @Query("SELECT COUNT(DISTINCT r.reservationId) FROM Reservation r " +
            "WHERE r.reservationId NOT IN (" +
            "  SELECT DISTINCT p.reservation.reservationId FROM Passenger p WHERE p.status = 'CONFIRMED'" +
            ")")
    long countFullyCancelledReservations();

    @Query("SELECT COALESCE(SUM(r.totalPrice), 0) FROM Reservation r")
    java.math.BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM Reservation r")
    java.math.BigDecimal sumTotalRefunds();
}