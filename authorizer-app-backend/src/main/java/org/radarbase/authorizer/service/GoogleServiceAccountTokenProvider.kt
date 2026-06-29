/*
 *  Copyright 2026 King's College London
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

package org.radarbase.authorizer.service

import com.google.auth.oauth2.GoogleCredentials
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.radarbase.authorizer.config.AuthorizerConfig
import org.slf4j.LoggerFactory
import java.io.FileInputStream

/**
 * Provides Google Cloud access tokens minted from a service-account JSON key file.
 */
@Singleton
class GoogleServiceAccountTokenProvider(
    @param:Context private val config: AuthorizerConfig,
) {
    private val credentials: GoogleCredentials? = run {
        val keyPath = config.googleHealth.serviceAccountKeyPath
        if (keyPath.isNullOrEmpty()) {
            logger.info("No googleHealth.serviceAccountKeyPath configured — subscription management is disabled.")
            null
        } else {
            try {
                GoogleCredentials.fromStream(FileInputStream(keyPath)).createScoped(SCOPES)
            } catch (ex: Exception) {
                logger.error("Failed to load service-account credentials from {}: {}", keyPath, ex.message)
                null
            }
        }
    }

    val isConfigured: Boolean
        get() = credentials != null

    /**
     * Returns a valid access token, refreshing if expired.
     * @throws IllegalStateException if no credentials are configured.
     */
    suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val creds = credentials ?: throw IllegalStateException("Service-account credentials are not configured")
        creds.refreshIfExpired()
        creds.accessToken.tokenValue
    }

    companion object {
        private val SCOPES = listOf("https://www.googleapis.com/auth/cloud-platform")
        private val logger = LoggerFactory.getLogger(GoogleServiceAccountTokenProvider::class.java)
    }
}
