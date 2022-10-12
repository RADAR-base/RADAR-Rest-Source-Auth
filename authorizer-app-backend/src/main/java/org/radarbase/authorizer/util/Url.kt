package org.radarbase.authorizer.util

import jakarta.ws.rs.core.UriBuilder

data class Url(
    var endPoint: String,
    var queryParams: Map<String, String?>,
) {
    fun getUrl(): String {
        return UriBuilder.fromUri(this.endPoint).apply {
            queryParams.asSequence()
                .filter { (_, v) -> v != null }
                .forEach { (k, v) -> queryParam(k, v) }
        }.build().toString()
    }
}
