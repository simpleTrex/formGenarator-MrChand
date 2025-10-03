package com.formgenerator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class MvcConfig implements WebMvcConfigurer {

	@Value("${application.fronend.path}")
	private String _app_frontend_path;

	@Value("${application.backend.path}")
	private String _app_backend_path;

	@Override
	public void addCorsMappings(@NonNull CorsRegistry registry) {
		registry.addMapping("/custom_form/**")
				.allowedOrigins(_app_frontend_path)/* .allowedOrigins(_app_backend_path) */
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.allowCredentials(true);
		registry.addMapping("/custom_form/auth/**")
				.allowedOrigins(_app_frontend_path)
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.allowCredentials(true);
	}
}
