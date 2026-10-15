package com.trackflow;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TrackflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrackflowApplication.class, args);
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}