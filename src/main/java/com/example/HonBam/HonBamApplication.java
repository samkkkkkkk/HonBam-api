package com.example.HonBam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

@SpringBootApplication
@EnableScheduling
public class HonBamApplication {

	public static void main(String[] args) {
		SpringApplication.run(HonBamApplication.class, args);
	}

}
