package org.radarbase.authorizer;

import java.util.List;
import java.util.stream.Stream;

import org.radarbase.authorizer.config.DeviceAuthorizerApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
@EnableConfigurationProperties({DeviceAuthorizerApplicationProperties.class})
public class RadarDeviceAuthorizerApplication {

    @Autowired
    private DeviceAuthorizerApplicationProperties authorizerApplicationProperties;

    public static void main(String[] args) {
        SpringApplication.run(RadarDeviceAuthorizerApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {

                CorsConfiguration corsConfiguration = authorizerApplicationProperties.getCors();
                Stream.of("/users/**", "/device-clients/**").forEach(p -> registry.addMapping(p)
                        .allowedOrigins(listToArray(corsConfiguration.getAllowedOrigins()))
                        .allowedMethods(listToArray(corsConfiguration.getAllowedMethods()))
                        .allowedHeaders(listToArray(corsConfiguration.getAllowedHeaders()))
                        .allowCredentials(corsConfiguration.getAllowCredentials()));

            }
        };
    }

    private String[] listToArray(List<String> list) {
        return list.stream().toArray(String[]::new);
    }

}
