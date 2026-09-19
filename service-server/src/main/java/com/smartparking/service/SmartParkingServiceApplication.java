package com.smartparking.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.smartparking")
public class SmartParkingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartParkingServiceApplication.class, args);
    }
}
