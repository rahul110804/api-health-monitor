package com.monitor.api_health_monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiHealthMonitorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiHealthMonitorApplication.class, args);
	}

}
