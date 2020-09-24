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

import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class StateStore(expiryTime: Duration = Duration.ofMinutes(5)) {

    private val expiryTime = expiryTime
    private val stateExpiry = mutableMapOf<State, Instant>()

    fun generateState(sourceType: String): State {
        val randomBytes = ByteArray(6)
        ThreadLocalRandom.current().nextBytes(randomBytes)
        val uniqueId: String = STATE_ENCODER.encodeToString(randomBytes)
        val state = State(uniqueId, sourceType)
        stateExpiry[state] = Instant.now().plus(expiryTime)
        return state
    }

    fun isValid(state: State): Boolean {
        val expired: Instant? = stateExpiry.remove(state)
        return expired != null && expired.isAfter(Instant.now())
    }

    companion object {
        private val STATE_ENCODER: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
    }

    data class State(val uuid: String, val sourceType: String) {
        override fun toString() = "state=$uuid&sourceType=$sourceType"
        companion object {
            fun toState(string: String) : State {
                val map = string.split("&")
                return State(map[0].split("=")[1], map[1].split("=")[1])
            }
        }
    }
}
