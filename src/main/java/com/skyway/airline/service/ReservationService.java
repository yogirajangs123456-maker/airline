package com.skyway.airline.service;

import com.skyway.airline.dto.BookingRequest;
import com.skyway.airline.entity.*;
import com.skyway.airline.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final FlightRepository flightRepository;
    private final UserRepository userRepository;
    private final SeatLockService seatLockService;

    private String generatePNR() {
        return "PNR" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 6);
    }

    @Transactional
    public Reservation createReservation(BookingRequest req, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Flight flight = flightRepository.findById(req.getFlightId())
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        boolean alreadyBooked = reservationRepository
                .existsByFlight_FlightIdAndSeatNumberAndStatus(
                        req.getFlightId(), req.getSeatNumber(), "CONFIRMED");
        if (alreadyBooked)
            throw new RuntimeException("Seat already booked!");

        boolean hasLock = seatLockService.acquireLock(
                req.getFlightId(), req.getSeatNumber(), user.getId());
        if (!hasLock)
            throw new RuntimeException("Seat is held by another user. Please choose a different seat.");

        // Bug #11: was an unbounded do-while loop — added retry cap
        String pnr = null;
        for (int i = 0; i < 10; i++) {
            String candidate = generatePNR();
            if (reservationRepository.findByPnr(candidate).isEmpty()) {
                pnr = candidate;
                break;
            }
        }
        if (pnr == null)
            throw new RuntimeException("Could not generate unique PNR. Please try again.");

        Reservation reservation = Reservation.builder()
                .pnr(pnr)
                .user(user)
                .flight(flight)
                .seatNumber(req.getSeatNumber())
                .passengerName(req.getPassengerName())
                .totalPrice(req.getTotalPrice())
                .status("CONFIRMED")
                .bookedAt(LocalDateTime.now())
                .build();

        Reservation saved;
        try {
            saved = reservationRepository.save(reservation);
        } catch (DataIntegrityViolationException e) {
            // Bug #7: catch duplicate seat race — DB unique constraint fires here
            throw new RuntimeException("Seat was just booked by another user. Please choose a different seat.");
        }

        flight.setAvailableSeats(flight.getAvailableSeats() - 1);
        flightRepository.save(flight);

        seatLockService.releaseLock(req.getFlightId(), req.getSeatNumber());
        return saved;
    }

    @Transactional
    public Reservation cancelReservation(String pnr, String userEmail) {
        Reservation reservation = reservationRepository.findByPnr(pnr)
                .orElseThrow(() -> new RuntimeException("PNR not found!"));

        if (!reservation.getUser().getEmail().equals(userEmail))
            throw new RuntimeException("This PNR does not belong to your account.");

        if ("CANCELLED".equals(reservation.getStatus()))
            throw new RuntimeException("This ticket is already cancelled.");

        reservation.setStatus("CANCELLED");
        reservation.setCancelledAt(LocalDateTime.now());

        Flight flight = reservation.getFlight();
        flight.setAvailableSeats(flight.getAvailableSeats() + 1);
        flightRepository.save(flight);

        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public List<String> getBookedSeats(Long flightId) {
        return reservationRepository.findBookedSeatsByFlightId(flightId);
    }

    @Transactional(readOnly = true)
    public List<Reservation> getUserReservations(String email) {
        return reservationRepository.findByUser_Email(email);
    }

    @Transactional(readOnly = true)
    public Reservation getReservationByPnr(String pnr, String email) {
        Reservation reservation = reservationRepository.findByPnr(pnr)
                .orElseThrow(() -> new RuntimeException("PNR not found!"));

        if (!reservation.getUser().getEmail().equals(email))
            throw new RuntimeException("This PNR does not belong to your account.");

        // Force-initialize lazy Flight proxy while the session is still open
        reservation.getFlight().getSource();

        return reservation;
    }
}