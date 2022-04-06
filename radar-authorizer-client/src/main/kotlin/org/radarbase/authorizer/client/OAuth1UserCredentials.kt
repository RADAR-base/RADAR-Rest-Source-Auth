package org.radarbase.authorizer.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OAuth1UserCredentials(
        @JsonProperty("accessToken") var accessToken: String
)


