package org.radarbase.authorizer.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import java.io.File;
import java.io.IOException;
import javax.naming.ConfigurationException;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigHelper.class);

  public static <T> T loadPropertiesFromFile(@NotNull String path,
      @NotNull TypeReference<T> typeReference)
      throws ConfigurationException {
    LOGGER.info("Loading config from {}", path);
    YAMLFactory yamlFactory = new YAMLFactory();
    try {
      YAMLParser yamlParser = yamlFactory.createParser(new File(path));
      T properties = new ObjectMapper(yamlFactory)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
          .readValue(yamlParser, typeReference);

      if (properties == null) {
        LOGGER.error("No valid configurations available on configured path. Please "
            + "check the syntax and file name");
        throw new ConfigurationException(
            "No valid configs are provided" + ".");
      }
      return properties;
    } catch (IOException e) {
      LOGGER.error("Could not successfully read config file at {}", path);
      throw new ConfigurationException("Could not successfully read config file at " + path);
    }
  }
}
