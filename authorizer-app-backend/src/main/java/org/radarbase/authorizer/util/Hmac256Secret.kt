package org.radarbase.authorizer.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class Hmac256Secret(val secret: String, val salt: ByteArray, val secretHash: ByteArray) {
    val isValid: Boolean by lazy {
        runHmac256(salt = salt, secret = secret.decodeFromBase64())
            .contentEquals(secretHash)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Hmac256Secret

        return secret == other.secret &&
            salt.contentEquals(other.salt) &&
            secretHash.contentEquals(other.secretHash)
    }

    override fun hashCode(): Int {
        var result = secret.hashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + secretHash.contentHashCode()
        return result
    }
}

private const val HMAC_SHA256 = "HmacSHA256"

private fun runHmac256(salt: ByteArray, secret: ByteArray) = Mac.getInstance(HMAC_SHA256).run {
    init(SecretKeySpec(salt, HMAC_SHA256))
    doFinal(secret)
}

fun Hmac256Secret(secretLength: Int, saltLength: Int = 6): Hmac256Secret {
    val secret = ByteArray(secretLength).randomize()
    val salt = ByteArray(saltLength).randomize()

    return Hmac256Secret(
        secret = secret.encodeToBase64(),
        salt = salt,
        secretHash = runHmac256(salt = salt, secret = secret),
    )
}
