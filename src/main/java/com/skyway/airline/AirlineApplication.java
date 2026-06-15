package com.skyway.airline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // ← Bug #2: was missing, @Scheduled jobs never ran
public class AirlineApplication {
	public static void main(String[] args) {
		SpringApplication.run(AirlineApplication.class, args);
	}
}