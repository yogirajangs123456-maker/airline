package com.skyway.airline.repository;

import com.skyway.airline.entity.SeatLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatLockRepository extends JpaRepository<SeatLock, Long> {

    Optional<SeatLock> findByFlightIdAndSeatNumber(Long flightId, String seatNumber);

    // Bug #3: @Transactional was missing — Spring Data requires it on @Modifying
    // queries
    @Modifying
    @Transactional
    @Query("DELETE FROM SeatLock sl WHERE sl.expiresAt < :now")
    void deleteExpiredLocks(@Param("now") LocalDateTime now);

    List<SeatLock> findByFlightId(Long flightId);
}