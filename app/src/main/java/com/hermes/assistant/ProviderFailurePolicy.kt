package com.hermes.assistant

import java.io.IOException
import java.util.concurrent.TimeoutException

internal enum class ProviderFailureKind {
    SUCCESS,
    PROVIDER_MODEL_UNAVAILABLE,
    PROVIDER_AUTH,
    PROVIDER_BAD_REQUEST,
    TRANSIENT,
    PROVIDER_EMPTY_RESPONSE
}

internal data class ProviderFailureClassification(
    val kind: ProviderFailureKind,
    val retryable: Boolean,
    val maxRetries: Int,
    val fallback: Boolean,
    val cooldown: Boolean = false,
    val userMessage: String? = null
)

/** Classification-only policy; retry timing and provider selection belong to the caller. */
internal fun classifyProviderFailure(
    httpStatus: Int?,
    body: String?,
    exception: Throwable?,
    contentBlank: Boolean
): ProviderFailureClassification {
    // RED-first stub: deliberately wrong until the implementation commit.
    return ProviderFailureClassification(
        kind = ProviderFailureKind.SUCCESS,
        retryable = false,
        maxRetries = 0,
        fallback = false
    )
}
