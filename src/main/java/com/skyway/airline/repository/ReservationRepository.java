package com.skyway.airline.repository;

import com.skyway.airline.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

        Optional<Reservation> findByPnr(String pnr);

        List<Reservation> findByUser_Email(String email);
}