package org.radarbase.authorizer.doa

import jakarta.inject.Provider
import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.Config
import org.radarbase.authorizer.doa.entity.RegistrationState
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.util.Hmac256Secret
import org.radarbase.authorizer.util.Hmac256Secret.Companion.encodeToBase64
import org.radarbase.authorizer.util.Hmac256Secret.Companion.randomize
import org.radarbase.jersey.hibernate.HibernateRepository
import java.time.Duration
import java.time.Instant
import javax.persistence.EntityManager
import javax.persistence.PersistenceException

class RegistrationRepository(
    @Context private val config: Config,
    @Context em: Provider<EntityManager>,
) : HibernateRepository(em) {

    private val tokenExpiryTime = Duration.ofMinutes(config.service.tokenExpiryTimeInMinutes)
    private val persistentTokenExpiryTime = Duration.ofMinutes(config.service.persistentTokenExpiryInMin)

    fun generate(
        user: RestSourceUser,
        secret: Hmac256Secret?,
        persistent: Boolean,
    ): RegistrationState? {
        val createdAt = Instant.now()
        val expiresAt = createdAt + if (persistent) persistentTokenExpiryTime else tokenExpiryTime
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

    operator fun get(token: String): RegistrationState? = transact {
        find(RegistrationState::class.java, token)
    }

    fun cleanUp(): Int = transact {
        val cb = criteriaBuilder

        // create delete
        val deleteQuery = cb.createCriteriaDelete(RegistrationState::class.java).apply {
            val tokenType = from(RegistrationState::class.java)
            where(cb.lessThan(tokenType["expiresAt"], Instant.now()))
        }

        // perform update
        createQuery(deleteQuery).executeUpdate()
    }

    operator fun minusAssign(token: String) = remove(token)

    operator fun minusAssign(registrationState: RegistrationState) = remove(registrationState)

    fun remove(registrationState: RegistrationState): Unit = transact {
        remove(registrationState)
    }

    fun remove(token: String): Unit = transact {
        val state = find(RegistrationState::class.java, token)
        remove(state)
    }

    companion object {
        fun randomStrings(numberOfBytes: Int): Sequence<String> =
            generateSequence { ByteArray(numberOfBytes).randomize().encodeToBase64() }
    }
}
