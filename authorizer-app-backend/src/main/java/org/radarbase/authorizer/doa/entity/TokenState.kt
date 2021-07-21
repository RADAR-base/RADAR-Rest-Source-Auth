package org.radarbase.authorizer.doa.entity

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.Immutable
import org.hibernate.proxy.HibernateProxy
import org.hibernate.proxy.HibernateProxyHelper
import java.time.Instant
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "token")
@Immutable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
class TokenState(
    @Id
    @Column
    val token: String,
    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(cascade = [], optional = false)
    val user: RestSourceUser,
    @Column(nullable = true)
    val salt: ByteArray?,
    @Column(name = "secret_hash", nullable = true)
    val secretHash: ByteArray?,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
    @Column(nullable = false)
    val persistent: Boolean,
) {
    val isValid: Boolean
        get() = Instant.now() < expiresAt

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        val otherClass = if (other is HibernateProxyHelper) {
            (other as HibernateProxy)
                .hibernateLazyInitializer
                .persistentClass
        } else other.javaClass

        if (javaClass != otherClass) return false

        other as TokenState

        return token == other.token &&
            user == other.user
    }

    override fun hashCode(): Int = Objects.hash(token)

    override fun toString(): String = "TokenState(token=$token, user=$user, expiresAt=$expiresAt)"
}
