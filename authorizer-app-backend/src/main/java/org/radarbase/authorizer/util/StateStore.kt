/*
 *  Copyright 2020 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.authorizer.util

import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.config.AuthorizerConfig
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

class StateStore(
    @Context config: AuthorizerConfig,
    @Context private val executor: ScheduledExecutorService,
) {
    private val expiryTime = Duration.ofMinutes(config.service.stateStoreExpiryInMin)
    private val store: ConcurrentHashMap<String, State> = ConcurrentHashMap()

    init {
        executor.scheduleAtFixedRate(
            ::clean,
            config.service.stateStoreExpiryInMin * 3,
            config.service.stateStoreExpiryInMin * 3,
            TimeUnit.MINUTES,
        )
    }

    fun generate(sourceType: String): State {
        return generateSequence { ByteArray(8).randomize().encodeToBase64() }
            .mapNotNull {
                val state = State(it, sourceType, Instant.now() + expiryTime)
                val existingValue = store.putIfAbsent(it, state)
                if (existingValue == null) state else null
            }
            .first()
    }

    operator fun get(stateId: String): State? = store.remove(stateId)

    private fun clean() {
        val now = Instant.now()
        store.keys.forEach { k ->
            store.compute(k) { _, v ->
                if (v != null && now < v.expiresAt) v else null
            }
        }
    }

    companion object {
        private val STATE_ENCODER: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

        private fun ByteArray.randomize(): ByteArray = apply {
            ThreadLocalRandom.current().nextBytes(this)
        }

        private fun ByteArray.encodeToBase64(): String = STATE_ENCODER.encodeToString(this)
    }

    data class State(val stateId: String, val sourceType: String, val expiresAt: Instant) {
        val isValid: Boolean
            get() = Instant.now() < expiresAt
    }
}
