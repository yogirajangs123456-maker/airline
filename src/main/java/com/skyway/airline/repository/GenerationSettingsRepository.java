package com.skyway.airline.repository;

import com.skyway.airline.entity.GenerationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationSettingsRepository extends JpaRepository<GenerationSettings, Long> {
}