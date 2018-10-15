package org.radarbase.authorizer;

import org.radarbase.authorizer.config.DeviceAuthorizerApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({DeviceAuthorizerApplicationProperties.class})
public class RadarDeviceAuthorizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(RadarDeviceAuthorizerApplication.class, args);
	}
}
