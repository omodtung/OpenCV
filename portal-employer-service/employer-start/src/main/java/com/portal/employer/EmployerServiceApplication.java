package com.portal.employer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.portal.employer")
public class EmployerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployerServiceApplication.class, args);
    }
}