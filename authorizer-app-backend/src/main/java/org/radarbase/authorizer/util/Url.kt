package org.radarbase.authorizer.util

import javax.ws.rs.core.UriBuilder

data class Url(
    var endPoint: String,
    var queryParams: Map<String, String?>,
) {

    fun getUrl(): String {
        val uriBuilder = UriBuilder.fromUri(this.endPoint)
        for ((key, value) in queryParams) {
            if (value.isNullOrEmpty()) continue
            uriBuilder.queryParam(key, value)
        }
        return uriBuilder.build().toString()
    }
}


