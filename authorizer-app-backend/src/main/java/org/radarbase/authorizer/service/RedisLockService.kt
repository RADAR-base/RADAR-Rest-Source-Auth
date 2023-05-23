package org.radarbase.authorizer.service

import jakarta.persistence.LockTimeoutException
import jakarta.ws.rs.core.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.radarbase.authorizer.config.AuthorizerConfig
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.exceptions.JedisException
import redis.clients.jedis.params.SetParams
import java.io.IOException
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
    override suspend fun <T> runLocked(lockName: String, timeout: Duration, doRun: suspend () -> T): T {
        val lockKey = "$lockPrefix/$lockName.lock"
        val setParams = SetParams()
            .nx() // only set if not already set
            .px((timeout * 3).inWholeMilliseconds) // limit the duration based on expected lock time

        val startTime = System.nanoTime()
        val totalTime = timeout.inWholeNanoseconds
        var didAcquire = false
        val callerCoroutineContext = currentCoroutineContext()
        return withJedis {
            while (System.nanoTime() - startTime < totalTime) {
                didAcquire = set(lockKey, uuid, setParams) != null
                if (didAcquire) {
                    break
                } else {
                    delay(POLL_PERIOD)
                }
            }
            if (!didAcquire) {
                throw LockTimeoutException()
            }
            try {
                withContext(callerCoroutineContext) {
                    doRun()
                }
            } finally {
                if (get(lockKey) == uuid) {
                    del(lockKey)
                }
            }
        }
    }

    @Throws(IOException::class)
    suspend fun <T> withJedis(
        routine: suspend Jedis.() -> T
    ): T = withContext(Dispatchers.IO) {
        try {
            jedisPool.resource.use {
                it.routine()
            }
        } catch (ex: JedisException) {
            throw IOException(ex)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RedisLockService::class.java)
        private val POLL_PERIOD = 250.milliseconds
    }
}
