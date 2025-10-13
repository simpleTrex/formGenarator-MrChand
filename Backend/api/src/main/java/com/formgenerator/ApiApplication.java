package com.formgenerator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan
public class ApiApplication {

	@Value("${application.fronend.path}")
	private String _app_frontend_path;

	private static ApplicationContext applicationContext;

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

}
