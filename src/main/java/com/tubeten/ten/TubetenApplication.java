package com.tubeten.ten;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TubetenApplication {
	public static void main(String[] args) {
		SpringApplication.run(TubetenApplication.class, args);
	}
}
