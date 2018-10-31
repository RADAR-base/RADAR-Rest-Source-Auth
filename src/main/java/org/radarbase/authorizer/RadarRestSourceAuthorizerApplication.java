/*
 *
 *  * Copyright 2018 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.authorizer;

import java.util.List;
import java.util.stream.Stream;

import org.radarbase.authorizer.config.RestSourceAuthorizerProperties;
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
@EnableConfigurationProperties({RestSourceAuthorizerProperties.class})
public class RadarRestSourceAuthorizerApplication {

    @Autowired
    private RestSourceAuthorizerProperties authorizerApplicationProperties;

    public static void main(String[] args) {
        SpringApplication.run(RadarRestSourceAuthorizerApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {

                CorsConfiguration corsConfiguration = authorizerApplicationProperties.getCors();
                Stream.of("/users/**", "/source-clients/**").forEach(p -> registry.addMapping(p)
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
