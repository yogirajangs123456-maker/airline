package com.skyway.airline.repository;

import com.skyway.airline.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByPnr(String pnr);

    List<Reservation> findByUser_Email(String email);

    boolean existsByFlight_FlightIdAndSeatNumberAndStatus(
            Long flightId, String seatNumber, String status);

    @Query("SELECT r.seatNumber FROM Reservation r " +
            "WHERE r.flight.flightId = :flightId AND r.status = 'CONFIRMED'")
    List<String> findBookedSeatsByFlightId(@Param("flightId") Long flightId);
}