package com.example.appchatbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class AppchatbackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppchatbackendApplication.class, args);
	}

}