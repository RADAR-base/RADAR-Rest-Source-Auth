package org.radarbase.authorizer.service

import java.time.Duration

interface LockService {
    fun <T> runLocked(lockName: String, timeout: Duration, doRun: () -> T): T
}
