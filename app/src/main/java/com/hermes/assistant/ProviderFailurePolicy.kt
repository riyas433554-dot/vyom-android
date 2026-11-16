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

/** Classifies provider outcomes; retry timing and provider selection belong to the caller. */
internal fun classifyProviderFailure(
    httpStatus: Int?,
    body: String?,
    exception: Throwable?,
    contentBlank: Boolean
): ProviderFailureClassification {
    if (exception != null) {
        return if (exception is IOException || exception is TimeoutException) {
            ProviderFailureClassification(
                kind = ProviderFailureKind.TRANSIENT,
                retryable = true,
                maxRetries = 2,
                fallback = true
            )
        } else {
            ProviderFailureClassification(
                kind = ProviderFailureKind.PROVIDER_BAD_REQUEST,
                retryable = false,
                maxRetries = 0,
                fallback = false,
                userMessage = "Provider request failed."
            )
        }
    }

    return when (httpStatus) {
        404 -> ProviderFailureClassification(
            kind = ProviderFailureKind.PROVIDER_MODEL_UNAVAILABLE,
            retryable = false,
            maxRetries = 0,
            fallback = true,
            cooldown = true
        )
        401, 403 -> ProviderFailureClassification(
            kind = ProviderFailureKind.PROVIDER_AUTH,
            retryable = false,
            maxRetries = 0,
            fallback = true,
            userMessage = "Provider key is invalid or unauthorized."
        )
        400 -> ProviderFailureClassification(
            kind = ProviderFailureKind.PROVIDER_BAD_REQUEST,
            retryable = false,
            maxRetries = 0,
            fallback = false,
            userMessage = "Provider rejected the request (HTTP 400)."
        )
        408, 425, 429, 500, 502, 503, 504 -> ProviderFailureClassification(
            kind = ProviderFailureKind.TRANSIENT,
            retryable = true,
            maxRetries = 2,
            fallback = true
        )
        in 200..299 -> if (contentBlank) {
            ProviderFailureClassification(
                kind = ProviderFailureKind.PROVIDER_EMPTY_RESPONSE,
                retryable = true,
                maxRetries = 1,
                fallback = true
            )
        } else {
            ProviderFailureClassification(
                kind = ProviderFailureKind.SUCCESS,
                retryable = false,
                maxRetries = 0,
                fallback = false
            )
        }
        null -> ProviderFailureClassification(
            kind = ProviderFailureKind.PROVIDER_BAD_REQUEST,
            retryable = false,
            maxRetries = 0,
            fallback = false,
            userMessage = "Provider returned no response status."
        )
        else -> ProviderFailureClassification(
            kind = ProviderFailureKind.PROVIDER_BAD_REQUEST,
            retryable = false,
            maxRetries = 0,
            fallback = false,
            userMessage = "Provider rejected the request (HTTP $httpStatus)."
        )
    }
}
