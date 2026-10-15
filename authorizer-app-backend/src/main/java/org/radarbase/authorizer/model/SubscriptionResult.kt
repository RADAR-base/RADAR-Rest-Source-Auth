package org.radarbase.authorizer.model

sealed interface SubscriptionResult {
    /** The subscription is in the desired state (created, or deleted or already gone). */
    data class Success(val name: String?) : SubscriptionResult

    /**
     * Create found a subscription with the same id at Google (HTTP 409). It may be orphaned: after a
     * user's grant expires (mainly while the OAuth client is in "Testing" mode, where grants last 7
     * days), Google keeps the subscription but stops delivering, hides it from the list, and still
     * rejects a new create with this id.
     */
    data object AlreadyExists : SubscriptionResult

    /** No service account configured; the call was not attempted. */
    data object NotConfigured : SubscriptionResult

    /** Retryable failure (network, 429, 5xx). The caller should retry later. */
    data class TransientFailure(val message: String) : SubscriptionResult

    /** Non-retryable failure (e.g. 4xx other than 404/409). The caller should not blindly retry. */
    data class PermanentFailure(val code: Int, val body: String?) : SubscriptionResult

    val isSuccess: Boolean
        get() = this is Success
}
