package com.skyway.airline.controller;

import com.skyway.airline.entity.Reservation;
import com.skyway.airline.entity.User;
import com.skyway.airline.repository.ReservationRepository;
import com.skyway.airline.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAll() {
        List<User> users = userRepository.findAll();

        return users.stream().map(user -> {
            List<Reservation> userReservations = reservationRepository.findByUser_Email(user.getEmail());

            BigDecimal totalSpent = userReservations.stream()
                    .map(Reservation::getTotalPrice)
                    .filter(p -> p != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> row = new HashMap<>();
            row.put("id", user.getId());
            row.put("name", user.getName());
            row.put("email", user.getEmail());
            row.put("registrationDate", user.getCreatedAt());
            row.put("totalBookings", userReservations.size());
            row.put("totalAmountSpent", totalSpent);
            return row;
        }).toList();
    }
}