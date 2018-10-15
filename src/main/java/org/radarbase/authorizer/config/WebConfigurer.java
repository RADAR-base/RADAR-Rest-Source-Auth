package org.radarbase.authorizer.config;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebConfigurer implements WebMvcConfigurer {

    @Autowired
    private DeviceAuthorizerApplicationProperties authorizerApplicationProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        CorsConfiguration corsConfiguration = authorizerApplicationProperties.getCors();
        Stream.of("/users/**", "/device-clients/**")
                .forEach(p -> registry.addMapping(p)
                        .allowedOrigins(listToArray(corsConfiguration.getAllowedOrigins()))
                        .allowedMethods(listToArray(corsConfiguration.getAllowedMethods()))
                        .allowedHeaders(listToArray(corsConfiguration.getAllowedHeaders()))
                        .allowCredentials(corsConfiguration.getAllowCredentials()));
    }

     private String[] listToArray(List<String> list) {
        return list.stream().toArray(String[]::new);
     }
}