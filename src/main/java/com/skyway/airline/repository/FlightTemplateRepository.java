package com.skyway.airline.repository;

import com.skyway.airline.entity.FlightTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlightTemplateRepository extends JpaRepository<FlightTemplate, Long> {
    List<FlightTemplate> findByStatus(String status);
}