package org.radarbase.authorizer.service

import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.config.AuthorizerConfig
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.exceptions.JedisException
import redis.clients.jedis.params.SetParams
import java.io.IOException
import java.time.Duration
import java.util.*
import javax.persistence.LockTimeoutException

class RedisLockService(
    @Context config: AuthorizerConfig,
) : LockService {
    private val uuid: String = UUID.randomUUID().toString()
    private val lockPrefix = config.redis.lockPrefix
    private val jedisPool = JedisPool(config.redis.uri)

    init {
        logger.debug("Managing locks as ID {}", uuid)
    }

    /**
     * @throws LockTimeoutException if the lock cannot be acquired.
     */
    override fun <T> runLocked(lockName: String, timeout: Duration, doRun: () -> T): T {
        val lockKey = "$lockPrefix/$lockName.lock"
        val setParams = SetParams()
            .nx() // only set if not already set
            .px(
                timeout
                    .multipliedBy(3L)
                    .toMillis()
            ) // limit the duration based on expected lock time

        val startTime = System.nanoTime()
        val totalTime = timeout.toNanos()
        var didAcquire = false
        return withJedis {
            while (System.nanoTime() - startTime < totalTime) {
                didAcquire = set(lockKey, uuid, setParams) != null
                if (didAcquire) {
                    break
                } else {
                    Thread.sleep(POLL_PERIOD)
                }
            }
            if (!didAcquire) {
                throw LockTimeoutException()
            }
            try {
                doRun()
            } finally {
                if (get(lockKey) == uuid) {
                    del(lockKey)
                }
            }
        }
    }

    @Throws(IOException::class)
    fun <T> withJedis(routine: Jedis.() -> T): T {
        return try {
            jedisPool.resource.use {
                it.routine()
            }
        } catch (ex: JedisException) {
            throw IOException(ex)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RedisLockService::class.java)
        private const val POLL_PERIOD = 250L
    }
}
