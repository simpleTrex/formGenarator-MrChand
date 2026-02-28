package com.adaptivebp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = { "com.adaptivebp", "com.formgenerator.platform.workflow" })
@EnableMongoRepositories(basePackages = { "com.adaptivebp", "com.formgenerator.platform.workflow" })
public class AdaptiveBpApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdaptiveBpApplication.class, args);
    }
}
