package com.tubeten.ten;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.tubeten.ten.api.repository")
@EntityScan(basePackages = "com.tubeten.ten.domain")
@EnableScheduling
public class TubetenApplication {
	public static void main(String[] args) {
		SpringApplication.run(TubetenApplication.class, args);
	}
}
