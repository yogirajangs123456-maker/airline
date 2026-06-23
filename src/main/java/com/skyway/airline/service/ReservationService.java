package com.skyway.airline.service;

import com.skyway.airline.dto.BookingRequest;
import com.skyway.airline.entity.*;
import com.skyway.airline.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final PassengerRepository passengerRepository;
    private final FlightRepository flightRepository;
    private final UserRepository userRepository;
    private final SeatLockService seatLockService;
    private final EmailService emailService;

    private static final int MAX_PASSENGERS_PER_BOOKING = 6;

    private String generatePNR() {
        return "PNR" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 6);
    }

    @Transactional
    public Reservation createReservation(BookingRequest req, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Flight flight = flightRepository.findById(req.getFlightId())
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        // ── ADDED: block booking if flight is not in a bookable status ────────
        if (!flight.isBookable()) {
            throw new RuntimeException(
                    "This flight is no longer available for booking (status: " + flight.getStatus() + ").");
        }
        // ─────────────────────────────────────────────────────────────────────

        List<BookingRequest.PassengerSeatRequest> passengerReqs = req.getPassengers();

        if (passengerReqs == null || passengerReqs.isEmpty())
            throw new RuntimeException("At least one passenger is required.");

        if (passengerReqs.size() > MAX_PASSENGERS_PER_BOOKING)
            throw new RuntimeException("Maximum " + MAX_PASSENGERS_PER_BOOKING + " passengers allowed per booking.");

        List<String> requestedSeats = passengerReqs.stream()
                .map(BookingRequest.PassengerSeatRequest::getSeatNumber)
                .toList();
        if (requestedSeats.stream().distinct().count() != requestedSeats.size())
            throw new RuntimeException("Duplicate seat numbers in the same booking are not allowed.");

        List<String> alreadyBooked = passengerRepository.findBookedSeatsByFlightId(req.getFlightId());
        for (String seat : requestedSeats) {
            if (alreadyBooked.contains(seat))
                throw new RuntimeException("Seat " + seat + " is already booked!");
        }

        List<String> lockedSoFar = new ArrayList<>();
        for (String seat : requestedSeats) {
            boolean hasLock = seatLockService.acquireLock(req.getFlightId(), seat, user.getId());
            if (!hasLock) {
                lockedSoFar.forEach(s -> seatLockService.releaseLock(req.getFlightId(), s));
                throw new RuntimeException(
                        "Seat " + seat + " is held by another user. Please choose a different seat.");
            }
            lockedSoFar.add(seat);
        }

        if (flight.getAvailableSeats() < passengerReqs.size())
            throw new RuntimeException("Not enough available seats on this flight.");

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
                .totalPrice(req.getTotalPrice())
                .bookedAt(LocalDateTime.now())
                .build();

        Reservation savedReservation;
        try {
            savedReservation = reservationRepository.save(reservation);
        } catch (DataIntegrityViolationException e) {
            lockedSoFar.forEach(s -> seatLockService.releaseLock(req.getFlightId(), s));
            throw new RuntimeException("Could not create booking. Please try again.");
        }

        List<Passenger> passengers = new ArrayList<>();
        for (BookingRequest.PassengerSeatRequest p : passengerReqs) {
            passengers.add(Passenger.builder()
                    .reservation(savedReservation)
                    .passengerName(p.getPassengerName())
                    .seatNumber(p.getSeatNumber())
                    .status("CONFIRMED")
                    .build());
        }
        passengerRepository.saveAll(passengers);
        savedReservation.setPassengers(passengers);

        flight.setAvailableSeats(flight.getAvailableSeats() - passengerReqs.size());
        flightRepository.save(flight);

        lockedSoFar.forEach(s -> seatLockService.releaseLock(req.getFlightId(), s));

        emailService.sendBookingConfirmation(savedReservation);

        return savedReservation;
    }

    @Transactional
    public Reservation cancelPassengers(String pnr, List<Long> passengerIds, String userEmail) {
        Reservation reservation = reservationRepository.findByPnr(pnr)
                .orElseThrow(() -> new RuntimeException("PNR not found!"));

        if (!reservation.getUser().getEmail().equals(userEmail))
            throw new RuntimeException("This PNR does not belong to your account.");

        List<Passenger> targetPassengers = reservation.getPassengers().stream()
                .filter(p -> passengerIds.contains(p.getPassengerId()))
                .toList();

        if (targetPassengers.isEmpty())
            throw new RuntimeException("No matching passengers found for cancellation.");

        BigDecimal perSeatPrice = reservation.getTotalPrice()
                .divide(BigDecimal.valueOf(reservation.getPassengers().size()), 2, java.math.RoundingMode.HALF_UP);

        BigDecimal newlyRefunded = BigDecimal.ZERO;
        Flight flight = reservation.getFlight();

        for (Passenger p : targetPassengers) {
            if ("CANCELLED".equals(p.getStatus()))
                continue;
            p.setStatus("CANCELLED");
            newlyRefunded = newlyRefunded.add(perSeatPrice);
            flight.setAvailableSeats(flight.getAvailableSeats() + 1);
        }

        passengerRepository.saveAll(targetPassengers);
        flightRepository.save(flight);

        reservation.setRefundAmount(reservation.getRefundAmount().add(newlyRefunded));

        boolean allCancelled = reservation.getPassengers().stream()
                .allMatch(p -> "CANCELLED".equals(p.getStatus()));
        if (allCancelled) {
            reservation.setCancelledAt(LocalDateTime.now());
        }

        Reservation saved = reservationRepository.save(reservation);

        emailService.sendCancellationConfirmation(saved, targetPassengers, newlyRefunded);

        return saved;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateRefundPreview(String pnr, List<Long> passengerIds, String userEmail) {
        Reservation reservation = reservationRepository.findByPnr(pnr)
                .orElseThrow(() -> new RuntimeException("PNR not found!"));

        if (!reservation.getUser().getEmail().equals(userEmail))
            throw new RuntimeException("This PNR does not belong to your account.");

        BigDecimal perSeatPrice = reservation.getTotalPrice()
                .divide(BigDecimal.valueOf(reservation.getPassengers().size()), 2, java.math.RoundingMode.HALF_UP);

        long eligibleCount = reservation.getPassengers().stream()
                .filter(p -> passengerIds.contains(p.getPassengerId()) && "CONFIRMED".equals(p.getStatus()))
                .count();

        return perSeatPrice.multiply(BigDecimal.valueOf(eligibleCount));
    }

    @Transactional(readOnly = true)
    public List<String> getBookedSeats(Long flightId) {
        return passengerRepository.findBookedSeatsByFlightId(flightId);
    }

    @Transactional(readOnly = true)
    public List<Reservation> getUserReservations(String email) {
        List<Reservation> reservations = reservationRepository.findByUser_Email(email);
        reservations.forEach(this::forceLoadLazyFields);
        return reservations;
    }

    @Transactional(readOnly = true)
    public Reservation getReservationByPnr(String pnr, String email) {
        Reservation reservation = reservationRepository.findByPnr(pnr)
                .orElseThrow(() -> new RuntimeException("PNR not found!"));

        if (!reservation.getUser().getEmail().equals(email))
            throw new RuntimeException("This PNR does not belong to your account.");

        forceLoadLazyFields(reservation);
        return reservation;
    }

    private void forceLoadLazyFields(Reservation r) {
        r.getUser().getEmail();
        r.getFlight().getSource();
        r.getPassengers().forEach(p -> p.getStatus());
    }
}