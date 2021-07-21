package org.radarbase.authorizer.service

import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.api.Token
import org.radarbase.authorizer.doa.TokenRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.util.Hmac256Secret
import org.radarbase.jersey.exception.HttpInternalServerException

class TokenService(
    @Context private val tokenRepository: TokenRepository,
) {
    fun generate(user: RestSourceUser, persistent: Boolean): Token {
        val secret = if (persistent) {
            Hmac256Secret.generate(secretLength = 12, saltLength = 6)
        } else {
            Hmac256Secret.generate(secretLength = 6, saltLength = 3)
        }
        val tokenState = tokenRepository.generate(user, secret, persistent) ?: throw HttpInternalServerException("token_not_generated", "Failed to generate token.")

        return Token(
            token = tokenState.token,
            secret = secret.secret,
            userId = tokenState.user.id?.toString() ?: throw HttpInternalServerException("token_incomplete", "Failed to generate complete token."),
            expiresAt = tokenState.expiresAt,
            persistent = tokenState.persistent,
        )
    }
}
