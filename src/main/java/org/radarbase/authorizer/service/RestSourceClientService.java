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

import com.fasterxml.jackson.core.type.TypeReference;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.radarbase.authorizer.config.ConfigHelper;
import org.radarbase.authorizer.config.RestSourceClientConfig;
import org.radarbase.authorizer.config.RestSourceAuthorizerProperties;
import org.radarbase.authorizer.service.dto.RestSourceAccessToken;
import org.radarbase.authorizer.service.dto.RestSourceClientDetailsDTO;
import org.radarbase.authorizer.service.dto.RestSourceClients;
import org.radarbase.authorizer.webapp.exception.BadGatewayException;
import org.radarbase.authorizer.webapp.exception.InvalidSourceTypeException;
import org.radarbase.authorizer.webapp.exception.TokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RestSourceClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestSourceClientService.class);

    @Autowired
    private RestSourceAuthorizerProperties restSourceAuthorizerProperties;

    private OkHttpClient client;

    private ObjectMapper mapper;

    // contains private data
    private Map<String, RestSourceClientConfig> configMap;

    // contains sharable public data
    private Map<String, RestSourceClientDetailsDTO> clientDetailsDTOMap;

    @PostConstruct
    public void init() throws ConfigurationException {
        String path = restSourceAuthorizerProperties.getSourceClientsFilePath();
        if (Objects.isNull(path) || path.equals("")) {
            LOGGER.info("No source clients file specified, not loading source clients");
            return;
        }
        List<RestSourceClientConfig> restSourceClientConfigs = loadDeviceClientConfigs(path);

        this.configMap = restSourceClientConfigs.stream()
                .collect(Collectors.toMap(RestSourceClientConfig::getSourceType, p -> p));
        this.clientDetailsDTOMap = restSourceClientConfigs.stream()
                .collect(Collectors.toMap(RestSourceClientConfig::getSourceType,
                        RestSourceClientDetailsDTO::new));

        LOGGER.info("Source client configs loaded...");
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.mapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    }

    private List<RestSourceClientConfig> loadDeviceClientConfigs(@NotNull String path) throws
            ConfigurationException {
        org.radarbase.authorizer.config.RestSourceClients restSourceClients =
            ConfigHelper.loadPropertiesFromFile(path,
                new TypeReference<org.radarbase.authorizer.config.RestSourceClients>() {});

        if (restSourceClients.getRestSourceClients() == null) {
            LOGGER.error("No valid configurations available on configured path. Please "
                    + "check the syntax and file name");
            throw new ConfigurationException(
                    "No valid source-client configs are provided" + ".");
        }
        return restSourceClients.getRestSourceClients();
    }


    public RestSourceClients getAllRestSourceClientDetails() {
        return new RestSourceClients()
                .sourceClients(new ArrayList<>(this.clientDetailsDTOMap.values()));
    }

    public List<String> getAvailableDeviceTypes() {
        return new ArrayList<>(this.clientDetailsDTOMap.keySet());
    }

    private RestSourceClientConfig getClientAuthorizationConfig(String sourceType) {
        if (this.configMap.containsKey(sourceType)) {
            return this.configMap.get(sourceType);
        } else {
            throw new InvalidSourceTypeException(sourceType);
        }
    }

    public RestSourceClientDetailsDTO getAllRestSourceClientDetails(String sourceType) {
        return this.clientDetailsDTOMap.get(sourceType);
    }


    RestSourceAccessToken getAccessTokenWithAuthorizeCode(String code, String sourceType) {

        RestSourceClientConfig authorizationConfig = getClientAuthorizationConfig(sourceType);
        FormBody form = new FormBody.Builder()
                .add("code", code)
                .add("grant_type", "authorization_code")
                .add("client_id", authorizationConfig.getClientId())
                .build();
        LOGGER.info("Requesting access token with authorization code");
        return processTokenRequest(form, authorizationConfig);

    }

    private RestSourceAccessToken processTokenRequest(FormBody form,
            RestSourceClientConfig authorizationConfig) {
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
                    return mapper.readValue(responseBody.string(), RestSourceAccessToken.class);
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

    RestSourceAccessToken refreshToken(String refreshToken, String sourceType) {
        FormBody form = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build();
        LOGGER.info("Requesting to refreshToken");
        return processTokenRequest(form, getClientAuthorizationConfig(sourceType));
    }

    boolean revokeToken(String accessToken, String sourceType) {

        RestSourceClientConfig authorizationConfig = getClientAuthorizationConfig(sourceType);
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
