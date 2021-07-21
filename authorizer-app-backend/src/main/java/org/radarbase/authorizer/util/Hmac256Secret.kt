package org.radarbase.authorizer.util

import java.security.SecureRandom
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class Hmac256Secret(val secret: String, val salt: ByteArray, val secretHash: ByteArray) {
    val isValid: Boolean
        get() = runHmac256(salt = salt, secret = secret.decodeFromBase64())
            .contentEquals(secretHash)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Hmac256Secret

        return secret == other.secret
            && salt.contentEquals(other.salt)
            && secretHash.contentEquals(other.secretHash)
    }

    override fun hashCode(): Int {
        var result = secret.hashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + secretHash.contentHashCode()
        return result
    }

    companion object {
        private const val HMAC_SHA256 = "HmacSHA256"

        private val RANDOM = SecureRandom()
        private val STATE_ENCODER: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
        private val STATE_DECODER: Base64.Decoder = Base64.getUrlDecoder()

        private fun runHmac256(salt: ByteArray, secret: ByteArray) = Mac.getInstance(HMAC_SHA256).run {
            init(SecretKeySpec(salt, HMAC_SHA256))
            doFinal(secret)
        }

        fun generate(secretLength: Int, saltLength: Int = 6): Hmac256Secret {
            val secret = ByteArray(secretLength).randomize()
            val salt = ByteArray(saltLength).randomize()

            return Hmac256Secret(
                secret = secret.encodeToBase64(),
                salt = salt,
                secretHash = runHmac256(salt = salt, secret = secret),
            )
        }

        fun ByteArray.randomize(): ByteArray = apply {
            RANDOM.nextBytes(this)
        }

        fun ByteArray.encodeToBase64(): String = STATE_ENCODER.encodeToString(this)
        fun String.decodeFromBase64(): ByteArray = STATE_DECODER.decode(this)
    }
}
