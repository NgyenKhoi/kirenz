package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.example.demo.repository.jpa")
@EnableMongoRepositories(basePackages = "com.example.demo.repository.mongo")
public class KirenzApplication {

	public static void main(String[] args) {
		SpringApplication.run(KirenzApplication.class, args);
	}

}
