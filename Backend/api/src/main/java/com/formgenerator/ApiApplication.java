package com.formgenerator; //declare the package 

import org.springframework.beans.factory.annotation.Value; //inject configuration values
import org.springframework.boot.SpringApplication; //bootstrap the application
import org.springframework.boot.autoconfigure.SpringBootApplication; //sprint boot's annotations
import org.springframework.context.ApplicationContext; //spring's context
import org.springframework.context.annotation.ComponentScan; //component scanning like @Component, @Service, @Repository, etc.

@SpringBootApplication
@ComponentScan
public class ApiApplication {

	@Value("${application.fronend.path}")
	private String _app_frontend_path;

	private static ApplicationContext applicationContext;

	public static void main(String[] args) {
		ApiApplication.applicationContext = SpringApplication.run(ApiApplication.class, args);
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

}
