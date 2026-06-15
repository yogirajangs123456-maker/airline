package com.skyway.airline.service;

import com.skyway.airline.entity.SeatLock;
import com.skyway.airline.repository.SeatLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final SeatLockRepository seatLockRepository;

    @Value("${app.seat.lock.minutes:10}")
    private int lockMinutes;

    /**
     * Acquires a pessimistic lock on a seat.
     * Returns true if lock was granted, false if another user holds it.
     */
    @Transactional
    public boolean acquireLock(Long flightId, String seatNumber, Long userId) {
        // Clean expired locks first
        seatLockRepository.deleteExpiredLocks(LocalDateTime.now());

        Optional<SeatLock> existing = seatLockRepository.findByFlightIdAndSeatNumber(flightId, seatNumber);

        if (existing.isPresent()) {
            SeatLock lock = existing.get();
            // If the existing lock belongs to this user, refresh it
            if (lock.getUserId().equals(userId)) {
                lock.setLockedAt(LocalDateTime.now());
                lock.setExpiresAt(LocalDateTime.now().plusMinutes(lockMinutes));
                seatLockRepository.save(lock);
                return true;
            }
            // Another user holds a valid lock
            return false;
        }

        // Create new lock
        SeatLock newLock = SeatLock.builder()
                .flightId(flightId)
                .seatNumber(seatNumber)
                .userId(userId)
                .lockedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(lockMinutes))
                .build();

        try {
            seatLockRepository.save(newLock);
            return true;
        } catch (Exception e) {
            // Unique constraint violated — another concurrent request got the lock
            return false;
        }
    }

    @Transactional
    public void releaseLock(Long flightId, String seatNumber) {
        seatLockRepository.findByFlightIdAndSeatNumber(flightId, seatNumber)
                .ifPresent(seatLockRepository::delete);
    }

    public List<String> getLockedSeats(Long flightId) {
        seatLockRepository.deleteExpiredLocks(LocalDateTime.now());
        return seatLockRepository.findByFlightId(flightId)
                .stream()
                .map(SeatLock::getSeatNumber)
                .toList();
    }

    // Auto-cleanup every minute
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cleanExpiredLocks() {
        seatLockRepository.deleteExpiredLocks(LocalDateTime.now());
    }
}