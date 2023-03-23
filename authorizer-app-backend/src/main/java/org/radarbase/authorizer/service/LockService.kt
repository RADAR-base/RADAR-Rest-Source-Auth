package org.radarbase.authorizer.service

import kotlin.time.Duration

interface LockService {
    fun <T> runLocked(lockName: String, timeout: Duration, doRun: () -> T): T
}
