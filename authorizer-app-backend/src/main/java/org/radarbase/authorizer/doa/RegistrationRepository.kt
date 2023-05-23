package org.radarbase.authorizer.doa

import jakarta.inject.Provider
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceException
import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.entity.RegistrationState
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.util.Hmac256Secret
import org.radarbase.authorizer.util.Hmac256Secret.Companion.encodeToBase64
import org.radarbase.authorizer.util.Hmac256Secret.Companion.randomize
import org.radarbase.jersey.hibernate.HibernateRepository
import org.radarbase.jersey.service.AsyncCoroutineService
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

class RegistrationRepository(
    @Context private val config: AuthorizerConfig,
    @Context em: Provider<EntityManager>,
    @Context asyncService: AsyncCoroutineService,
) : HibernateRepository(em, asyncService) {

    private val tokenExpiryTime = config.service.tokenExpiryTimeInMinutes.minutes
    private val persistentTokenExpiryTime = config.service.persistentTokenExpiryInMin.minutes

    suspend fun generate(
        user: RestSourceUser,
        secret: Hmac256Secret?,
        persistent: Boolean,
    ): RegistrationState? {
        val createdAt = Instant.now()
        val expiryTime = if (persistent) persistentTokenExpiryTime else tokenExpiryTime
        val expiresAt = createdAt.plusMillis(expiryTime.inWholeMilliseconds)
        val numberOfBytes = if (persistent) 18 else 9
        return randomStrings(numberOfBytes)
            .take(10)
            .firstNotNullOfOrNull { token ->
                try {
                    transact {
                        RegistrationState(
                            token = token,
                            user = user,
                            createdAt = createdAt,
                            expiresAt = expiresAt,
                            salt = secret?.salt,
                            secretHash = secret?.secretHash,
                            persistent = persistent,
                        ).also { persist(it) }
                    }
                } catch (ex: PersistenceException) {
                    // continue
                    null
                }
            }
    }

    suspend fun get(token: String): RegistrationState? = transact {
        find(RegistrationState::class.java, token)
    }

    suspend fun cleanUp(): Int = transact {
        val cb = criteriaBuilder

        // create delete
        val deleteQuery = cb.createCriteriaDelete(RegistrationState::class.java).apply {
            val tokenType = from(RegistrationState::class.java)
            where(cb.lessThan(tokenType["expiresAt"], Instant.now()))
        }

        // perform update
        createQuery(deleteQuery).executeUpdate()
    }

    suspend fun remove(registrationState: RegistrationState): Unit = transact {
        remove(registrationState)
    }

    suspend fun remove(token: String): Unit = transact {
        val state = find(RegistrationState::class.java, token)
        remove(state)
    }

    companion object {
        fun randomStrings(numberOfBytes: Int): Sequence<String> =
            generateSequence { ByteArray(numberOfBytes).randomize().encodeToBase64() }
    }
}
