package com.payguard.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class PayguardApplication {
	public static void main(String[] args) {
		System.setProperty("spring.config.import", "optional:classpath:.env[.properties]");
		SpringApplication.run(PayguardApplication.class, args);
	}
}