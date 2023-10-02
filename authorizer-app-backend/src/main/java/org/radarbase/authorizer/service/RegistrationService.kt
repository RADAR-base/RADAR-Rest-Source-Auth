package org.radarbase.authorizer.service

import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.api.RegistrationResponse
import org.radarbase.authorizer.doa.RegistrationRepository
import org.radarbase.authorizer.doa.entity.RegistrationState
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.util.Hmac256Secret
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.exception.HttpInternalServerException

class RegistrationService(
    @Context private val registrationRepository: RegistrationRepository,
) {
    suspend fun generate(user: RestSourceUser, persistent: Boolean): RegistrationResponse {
        val secret = if (persistent) {
            Hmac256Secret(secretLength = 12, saltLength = 6)
        } else {
            Hmac256Secret(secretLength = 6, saltLength = 3)
        }
        val tokenState = registrationRepository.generate(user, secret, persistent)
            ?: throw HttpInternalServerException("token_not_generated", "Failed to generate token.")

        return RegistrationResponse(
            token = tokenState.token,
            secret = secret.secret,
            userId = tokenState.user.id?.toString() ?: throw HttpInternalServerException("token_incomplete", "Failed to generate complete token."),
            createdAt = tokenState.createdAt,
            expiresAt = tokenState.expiresAt,
            persistent = tokenState.persistent,
            sourceType = tokenState.user.sourceType,
        )
    }

    suspend fun ensureRegistration(token: String): RegistrationState {
        val registration = registrationRepository.get(token)
            ?: throw HttpBadRequestException("registration_not_found", "State has expired or not found")
        if (!registration.isValid) throw HttpBadRequestException("registration_expired", "Token has expired")
        return registration
    }
}
