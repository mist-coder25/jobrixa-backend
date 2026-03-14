package com.jobrixa.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class JobrixaApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobrixaApiApplication.class, args);
	}

}
