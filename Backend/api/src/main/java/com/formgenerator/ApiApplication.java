package com.formgenerator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@ComponentScan
public class ApiApplication {

	@Value("${application.fronend.path}")
	private String _app_frontend_path;

	private static ApplicationContext applicationContext;

	public static void main(String[] args) {
		ApiApplication.applicationContext = SpringApplication.run(ApiApplication.class, args);
	}

	@Bean
	@Scope(value = "singleton")
	public WebMvcConfigurer corsConfigurer() {
		return new MvcConfig() {
			@Override
			public void addCorsMappings(@NonNull CorsRegistry registry) {
				registry.addMapping("/custom_form/**")
						.allowedOrigins(_app_frontend_path)
						.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
						.allowedHeaders("*")
						.allowCredentials(true);
			}
		};
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

}
