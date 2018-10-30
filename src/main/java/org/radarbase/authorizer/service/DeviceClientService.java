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

package org.radarbase.authorizer.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.naming.ConfigurationException;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.radarbase.authorizer.config.DeviceAuthorizationConfig;
import org.radarbase.authorizer.config.DeviceAuthorizerApplicationProperties;
import org.radarbase.authorizer.config.DeviceClients;
import org.radarbase.authorizer.service.dto.DeviceAccessToken;
import org.radarbase.authorizer.service.dto.DeviceClientDetailsDTO;
import org.radarbase.authorizer.service.dto.SourceClientsDTO;
import org.radarbase.authorizer.webapp.exception.BadGatewayException;
import org.radarbase.authorizer.webapp.exception.InvalidDeviceTypeException;
import org.radarbase.authorizer.webapp.exception.TokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceClientService.class);

    @Autowired
    private DeviceAuthorizerApplicationProperties deviceAuthorizerApplicationProperties;

    private OkHttpClient client;

    private ObjectMapper mapper;

    // contains private data
    private Map<String, DeviceAuthorizationConfig> configMap;

    // contains sharable public data
    private Map<String, DeviceClientDetailsDTO> clientDetailsDTOMap;

    @PostConstruct
    public void init() throws ConfigurationException {
        String path = deviceAuthorizerApplicationProperties.getDeviceClientsFilePath();
        if (Objects.isNull(path) || path.equals("")) {
            LOGGER.info("No device clients file specified, not loading device clients");
            return;
        }
        List<DeviceAuthorizationConfig> deviceAuthConfigs = loadDeviceClientConfigs(path);

        this.configMap = deviceAuthConfigs.stream()
                .collect(Collectors.toMap(DeviceAuthorizationConfig::getDeviceType, p -> p));
        this.clientDetailsDTOMap = deviceAuthConfigs.stream()
                .collect(Collectors.toMap(DeviceAuthorizationConfig::getDeviceType,
                        DeviceClientDetailsDTO::new));

        LOGGER.info("Device configs loaded...");
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    }

    private List<DeviceAuthorizationConfig> loadDeviceClientConfigs(@NotNull String path) throws
            ConfigurationException {
        LOGGER.info("Loading device client configs from {}", path);
        YAMLFactory yamlFactory = new YAMLFactory();
        try {
            YAMLParser yamlParser = yamlFactory.createParser(new File(path));
            MappingIterator<DeviceClients> mappingIterator = new ObjectMapper(yamlFactory)
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
                    .readValues(yamlParser, DeviceClients.class);

            if (mappingIterator.hasNext()) {
                DeviceClients deviceClients = mappingIterator.next();
                if (deviceClients == null || deviceClients.getDeviceClients() == null) {
                    LOGGER.error("No valid configurations available on configred path. Please "
                            + "check the syntax and file name");
                    throw new ConfigurationException(
                            "No valid device-client configs are provided" + ".");
                }
                return deviceClients.getDeviceClients();
            } else {
                throw new ConfigurationException(
                        "No valid device-client configs are provided" + ".");
            }

        } catch (IOException e) {
            LOGGER.error("Could not successfully read config file at {}", path);
            throw new ConfigurationException("Could not successfully read config file at " + path);
        }
    }


    public SourceClientsDTO getAllDeviceClientDetails() {
        return new SourceClientsDTO()
                .sourceClients(new ArrayList<>(this.clientDetailsDTOMap.values()));
    }

    public List<String> getAvailableDeviceTypes() {
        return new ArrayList<>(this.clientDetailsDTOMap.keySet());
    }

    private DeviceAuthorizationConfig getClientAuthorizationConfig(String deviceType) {
        if (this.configMap.containsKey(deviceType)) {
            return this.configMap.get(deviceType);
        } else {
            throw new InvalidDeviceTypeException(deviceType);
        }
    }

    public DeviceClientDetailsDTO getAllDeviceClientDetails(String deviceType) {
        return this.clientDetailsDTOMap.get(deviceType);
    }


    DeviceAccessToken getAccessTokenWithAuthorizeCode(String code, String deviceType) {

        DeviceAuthorizationConfig authorizationConfig = getClientAuthorizationConfig(deviceType);
        FormBody form = new FormBody.Builder()
                .add("code", code)
                .add("grant_type", "authorization_code")
                .add("client_id", authorizationConfig.getClientId())
                .build();
        LOGGER.info("Requesting access token with authorization code");
        return processTokenRequest(form, authorizationConfig);

    }

    private DeviceAccessToken processTokenRequest(FormBody form,
            DeviceAuthorizationConfig authorizationConfig) {
        String credentials = Credentials
                .basic(authorizationConfig.getClientId(), authorizationConfig.getClientSecret());

        Request request = new Request.Builder().addHeader("Accept", "application/json")
                .addHeader("Authorization", credentials)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .url(authorizationConfig.getTokenEndpoint())
                .post(form)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new BadGatewayException("No response from server");
                }

                try {
                    return mapper.readValue(responseBody.string(), DeviceAccessToken.class);
                } catch (IOException e) {
                    throw new TokenException("Cannot read token response");
                }
            } else {
                throw new BadGatewayException(
                        "Failed to execute the request : Response-code :" + response.code()
                                + " received when requesting token from server with " + "message "
                                + response.message());
            }
        } catch (IOException e) {
            throw new BadGatewayException(e);
        }
    }

    DeviceAccessToken refreshToken(String refreshToken, String deviceType) {
        FormBody form = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build();
        LOGGER.info("Requesting to refreshToken");
        return processTokenRequest(form, getClientAuthorizationConfig(deviceType));
    }

    boolean revokeToken(String accessToken, String deviceType) {

        DeviceAuthorizationConfig authorizationConfig = getClientAuthorizationConfig(deviceType);
        FormBody form = new FormBody.Builder().add("token", accessToken).build();
        LOGGER.info("Requesting to revoke access token");
        String credentials = Credentials
                .basic(authorizationConfig.getClientId(), authorizationConfig.getClientSecret());

        Request request = new Request.Builder().addHeader("Accept", "application/json")
                .addHeader("Authorization", credentials)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .url(authorizationConfig.getTokenEndpoint()).post(form).build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            throw new BadGatewayException(e);
        }

    }
}
