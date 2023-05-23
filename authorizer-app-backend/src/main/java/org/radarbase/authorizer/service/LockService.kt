package org.radarbase.authorizer.service

import kotlin.time.Duration

interface LockService {
    suspend fun <T> runLocked(lockName: String, timeout: Duration, doRun: suspend () -> T): T
}
