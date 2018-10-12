package org.radarbase.authorizer.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.radarbase.authorizer.config.DeviceAuthorizationConfig;
import org.radarbase.authorizer.config.DeviceAuthorizerApplicationProperties;
import org.radarbase.authorizer.service.dto.DeviceAccessToken;
import org.radarbase.authorizer.service.dto.DeviceClientDetailsDTO;
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
    public void init() {
        this.configMap = deviceAuthorizerApplicationProperties.getDeviceAuthConfigs()
                .stream()
                .collect(Collectors.toMap(DeviceAuthorizationConfig::getDeviceType, p -> p));

        this.clientDetailsDTOMap = deviceAuthorizerApplicationProperties.getDeviceAuthConfigs()
                .stream()
                .collect(Collectors.toMap(DeviceAuthorizationConfig::getDeviceType,
                                DeviceClientDetailsDTO::new));
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    }

    public List<DeviceClientDetailsDTO> getAllDeviceClientDetails() {
        return new ArrayList<>(this.clientDetailsDTOMap.values());
    }

    public List<String> getAvailableDeviceTypes() {
        return new ArrayList<>(this.clientDetailsDTOMap.keySet());
    }

    private DeviceAuthorizationConfig getClientAuthorizationConfig(String deviceType) {
        if(this.configMap.containsKey(deviceType)) {
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
                .url(authorizationConfig.getTokenEndpoint()).post(form).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    throw new BadGatewayException("No response from server");
                }

                return mapper.readValue(responseBody.string(), DeviceAccessToken.class);
            } else {
                // TODO handle status codes from fitbut
                throw new BadGatewayException(
                        "Cannot get a valid token : Response-code :" + response.code()
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
        return processTokenRequest(form,  getClientAuthorizationConfig(deviceType));
    }
}
