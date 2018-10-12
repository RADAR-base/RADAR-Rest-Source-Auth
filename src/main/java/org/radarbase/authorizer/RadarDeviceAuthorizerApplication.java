package org.radarbase.authorizer;

import org.radarbase.authorizer.config.DeviceAuthorizerApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
@EnableConfigurationProperties({DeviceAuthorizerApplicationProperties.class})
public class RadarDeviceAuthorizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(RadarDeviceAuthorizerApplication.class, args);
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurerAdapter() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/users/**").allowedOrigins("http://localhost:4200")
				.allowedMethods("PUT", "POST", "GET", "OPTIONS", "DELETE");
				registry.addMapping("/device-clients/**").allowedOrigins("http://localhost:4200");
				registry.addMapping("/callback").allowedOrigins("http://localhost:4200");
			}
		};
	}
}
