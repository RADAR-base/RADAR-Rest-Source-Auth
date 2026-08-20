package org.radarbase.authorizer.model

/** A subscription as returned by Google's list endpoint. */
data class RemoteSubscription(
    val name: String,
    val user: String,
    val dataTypes: List<String>,
) {
    /** The Google health user id (the part after the "users/" prefix), or null if malformed. */
    val healthUserId: String?
        get() = user.removePrefix("users/").takeIf { it.isNotEmpty() && it != user }

    /**
     * Bare data-type tokens (e.g. "steps"), with Google's "users/{id}/dataTypes/" qualifier stripped.
     * Google stores/returns subscription data types as fully-qualified resource names like
     * `users/{userId}/dataTypes/daily-resting-heart-rate`, while config and create payloads use bare
     * tokens, normalise before comparing the two.
     */
    val dataTypeIds: List<String>
        get() = dataTypes.map { it.substringAfterLast('/') }
}
