package org.radarbase.authorizer.model

sealed interface SubscriptionResult {
    /** The subscription is in the desired state (created, already existed, or already deleted). */
    data class Success(val name: String?) : SubscriptionResult

    /** No service account configured; the call was not attempted. */
    data object NotConfigured : SubscriptionResult

    /** Retryable failure (network, 429, 5xx). The caller should retry later. */
    data class TransientFailure(val message: String) : SubscriptionResult

    /** Non-retryable failure (e.g. 4xx other than 404/409). The caller should not blindly retry. */
    data class PermanentFailure(val code: Int, val body: String?) : SubscriptionResult

    val isSuccess: Boolean
        get() = this is Success
}
