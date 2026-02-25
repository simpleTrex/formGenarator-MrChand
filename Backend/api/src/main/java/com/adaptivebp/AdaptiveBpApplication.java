package com.adaptivebp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.adaptivebp")
public class AdaptiveBpApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdaptiveBpApplication.class, args);
    }
}
