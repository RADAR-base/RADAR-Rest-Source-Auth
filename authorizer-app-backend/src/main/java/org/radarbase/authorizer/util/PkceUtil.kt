package org.radarbase.authorizer.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PkceUtil {
    private val secureRandom = SecureRandom()
    private val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()

    // code verifier length needs to be between 43-128 characters
    private const val CODE_VERIFIER_LENGTH = 64

    fun generateCodeVerifier(): String {
        return ByteArray(CODE_VERIFIER_LENGTH)
            .randomize()
            .encodeToBase64()
    }

    fun generateCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        return hash.encodeToBase64()
    }
}
