package org.radarbase.authorizer.util

import java.net.URLEncoder
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class OauthSignature(
        var endPoint: String,
        var params: MutableMap<String, String?>,
        var method: String,
        var clientSecret: String?,
        var tokenSecret: String?
) {

    fun getEncodedSignature(): String {
        val encodedUrl = URLEncoder.encode(this.endPoint)
        val encodedParams = URLEncoder.encode(mapToQuery(this.params))
        var signatureBase = "$method&$encodedUrl&$encodedParams"
        var key = "${this.clientSecret.orEmpty()}&${this.tokenSecret.orEmpty()}"
        val signatureEncoded = URLEncoder.encode(this.encodeSHA(key, signatureBase))
        return signatureEncoded;
    }

    fun encodeSHA(key: String, plaintext: String): String?{
        val result: String;
        val signingKey = SecretKeySpec(key.toByteArray(),"HmacSHA1");
        val mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        val rawHmac= mac.doFinal(plaintext.toByteArray());
        result = Base64.getEncoder().encodeToString(rawHmac);
        return result;
    }

    fun mapToQuery(map: MutableMap<String, String?>): String {
        val sb = StringBuilder()
        for ((key, value) in map) {
            if (value.isNullOrEmpty()) continue
            if (sb.length > 0) {
                sb.append('&')
            }
            sb.append(key).append('=').append(value)
        }
        return sb.toString()
    }
}


