package com.campus.campus_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CampusBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CampusBackendApplication.class, args);
	}

}
