package org.radarbase.authorizer.enhancer

import jakarta.inject.Singleton
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.radarbase.authorizer.service.LockService
import org.radarbase.authorizer.service.RedisLockService
import org.radarbase.jersey.enhancer.JerseyResourceEnhancer

class JedisResourceEnhancer : JerseyResourceEnhancer {
    override fun AbstractBinder.enhance() {
        bind(RedisLockService::class.java)
            .to(LockService::class.java)
            .`in`(Singleton::class.java)
    }
}
