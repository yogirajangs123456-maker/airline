package com.skyway.airline.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "generation_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Allowed values: 5, 10, 15, 30 */
    @Builder.Default
    private int windowDays = 10;
}