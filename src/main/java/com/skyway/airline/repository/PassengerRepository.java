package com.skyway.airline.repository;

import com.skyway.airline.entity.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    @Query("SELECT p.seatNumber FROM Passenger p " +
            "WHERE p.reservation.flight.flightId = :flightId AND p.status = 'CONFIRMED'")
    List<String> findBookedSeatsByFlightId(@Param("flightId") Long flightId);

    List<Passenger> findByReservation_ReservationId(Long reservationId);
}