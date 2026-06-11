package com.weai.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WeAiServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeAiServerApplication.class, args);
	}

}
