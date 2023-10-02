package org.radarbase.authorizer.util

import java.security.SecureRandom
import java.util.Base64

private val RANDOM = SecureRandom()
private val STATE_ENCODER: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

fun ByteArray.randomize(): ByteArray = apply {
    RANDOM.nextBytes(this)
}

fun ByteArray.encodeToBase64(): String = STATE_ENCODER.encodeToString(this)
fun String.decodeFromBase64(): ByteArray = Base64.getUrlDecoder().decode(this)
