package org.radarbase.authorizer.listener;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.radarbase.authorizer.config.DeviceAuthorizationConfig;
import org.radarbase.authorizer.service.dto.DeviceAccessToken;
import org.radarbase.authorizer.webapp.exception.BadGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceAuthorizerClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceAuthorizerClient.class);
    private final OkHttpClient client;
    private DeviceAuthorizationConfig authorizationConfig;
    ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);



    public DeviceAuthorizerClient(OkHttpClient okHttpClient, DeviceAuthorizationConfig authorizationConfig) {
        this.client = okHttpClient;
        this.authorizationConfig = authorizationConfig;
    }

    public String getAuthorizeRedirectUrl(String stateValue) {
        LOGGER.info("Building authorize url with state {}", stateValue);
        HttpUrl httpUrl =  HttpUrl.parse(authorizationConfig.getAuthorizationEndpoint())
                .newBuilder()
                .addQueryParameter("client_id", authorizationConfig.getClientId())
                .addQueryParameter("scope", authorizationConfig.getScope())
                .addQueryParameter("response_type" , "code")
                .addQueryParameter("state", stateValue)
                .build();
        return httpUrl.toString();

    }


}
