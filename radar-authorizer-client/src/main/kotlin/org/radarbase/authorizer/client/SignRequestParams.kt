package org.radarbase.authorizer.client

import com.fasterxml.jackson.annotation.JsonProperty

data class SignRequestParams(
    @JsonProperty("url") var url: String,
    @JsonProperty("method") var method: String,
    @JsonProperty("parameters") val parameters: Map<String, String>,
)
