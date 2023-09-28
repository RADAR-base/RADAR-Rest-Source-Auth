package org.radarbase.authorizer.util

import java.net.URLEncoder
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.text.Charsets.UTF_8

data class OauthSignature(
    var endPoint: String,
    var params: Map<String, String?>,
    var method: String,
    var clientSecret: String?,
    var tokenSecret: String?,
) {
    fun getEncodedSignature(): String {
        val encodedUrl = URLEncoder.encode(this.endPoint, UTF_8)
        val encodedParams = URLEncoder.encode(this.params.toQueryFormat(), UTF_8)
        val signatureBase = "$method&$encodedUrl&$encodedParams"
        val key = "${this.clientSecret.orEmpty()}&${this.tokenSecret.orEmpty()}"
        return URLEncoder.encode(encodeSHA(key, signatureBase), UTF_8)
    }

    private fun encodeSHA(key: String, plaintext: String): String? {
        val signingKey = SecretKeySpec(key.toByteArray(), "HmacSHA1")
        val rawHmac = Mac.getInstance("HmacSHA1")
            .run {
                init(signingKey)
                doFinal(plaintext.toByteArray())
            }
        return Base64.getEncoder().encodeToString(rawHmac)
    }

    companion object {
        private fun Map<String, String?>.toQueryFormat(): String =
            entries.joinToString("&") { (k, v) -> "$k=$v" }
    }
}
