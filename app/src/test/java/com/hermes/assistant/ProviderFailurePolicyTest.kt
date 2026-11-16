package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class ProviderFailurePolicyTest {
    private data class Case(
        val name: String,
        val status: Int?,
        val body: String? = null,
        val exception: Throwable? = null,
        val contentBlank: Boolean = false,
        val kind: ProviderFailureKind,
        val retryable: Boolean,
        val maxRetries: Int,
        val fallback: Boolean,
        val cooldown: Boolean = false,
        val userMessage: String? = null
    )

    @Test fun classifiesProviderFailuresFromStatusBodyAndException() {
        val cases = listOf(
            Case("404 empty body", 404, kind = ProviderFailureKind.PROVIDER_MODEL_UNAVAILABLE,
                retryable = false, maxRetries = 0, fallback = true, cooldown = true),
            Case("404 account-specific body", 404, "Not found for account",
                kind = ProviderFailureKind.PROVIDER_MODEL_UNAVAILABLE, retryable = false,
                maxRetries = 0, fallback = true, cooldown = true),
            Case("401", 401, "{\"error\":\"unauthorized\"}",
                kind = ProviderFailureKind.PROVIDER_AUTH, retryable = false, maxRetries = 0,
                fallback = true, userMessage = "Provider key is invalid or unauthorized."),
            Case("403", 403, "forbidden",
                kind = ProviderFailureKind.PROVIDER_AUTH, retryable = false, maxRetries = 0,
                fallback = true, userMessage = "Provider key is invalid or unauthorized."),
            Case("400", 400, "bad request",
                kind = ProviderFailureKind.PROVIDER_BAD_REQUEST, retryable = false,
                maxRetries = 0, fallback = false,
                userMessage = "Provider rejected the request (HTTP 400)."),
            Case("429", 429, "rate limited", kind = ProviderFailureKind.TRANSIENT,
                retryable = true, maxRetries = 2, fallback = true),
            Case("500", 500, kind = ProviderFailureKind.TRANSIENT, retryable = true,
                maxRetries = 2, fallback = true),
            Case("502", 502, kind = ProviderFailureKind.TRANSIENT, retryable = true,
                maxRetries = 2, fallback = true),
            Case("503", 503, kind = ProviderFailureKind.TRANSIENT, retryable = true,
                maxRetries = 2, fallback = true),
            Case("504", 504, kind = ProviderFailureKind.TRANSIENT, retryable = true,
                maxRetries = 2, fallback = true),
            Case("socket timeout", null, exception = SocketTimeoutException("timed out"),
                kind = ProviderFailureKind.TRANSIENT, retryable = true, maxRetries = 2, fallback = true),
            Case("timeout", null, exception = TimeoutException("timed out"),
                kind = ProviderFailureKind.TRANSIENT, retryable = true, maxRetries = 2, fallback = true),
            Case("I/O failure", null, exception = IOException("connection reset"),
                kind = ProviderFailureKind.TRANSIENT, retryable = true, maxRetries = 2, fallback = true),
            Case("200 blank content", 200, "   ", contentBlank = true,
                kind = ProviderFailureKind.PROVIDER_EMPTY_RESPONSE, retryable = true,
                maxRetries = 1, fallback = true),
            Case("200 slow response with content", 200, "{\"content\":\"complete\"}",
                contentBlank = false, kind = ProviderFailureKind.SUCCESS,
                retryable = false, maxRetries = 0, fallback = false)
        )

        cases.forEach { case ->
            val actual = classifyProviderFailure(
                case.status, case.body, case.exception, case.contentBlank
            )
            assertEquals("${case.name}: kind", case.kind, actual.kind)
            assertEquals("${case.name}: retryable", case.retryable, actual.retryable)
            assertEquals("${case.name}: maxRetries", case.maxRetries, actual.maxRetries)
            assertEquals("${case.name}: fallback", case.fallback, actual.fallback)
            assertEquals("${case.name}: cooldown", case.cooldown, actual.cooldown)
            assertEquals("${case.name}: safe user message", case.userMessage, actual.userMessage)
        }
    }

    @Test fun authMessageNeverIncludesResponseBodyOrCredential() {
        val secret = "nvidia-secret-must-never-appear"
        val result = classifyProviderFailure(
            401, "invalid key: $secret", null, contentBlank = false
        )
        assertEquals("Provider key is invalid or unauthorized.", result.userMessage)
        assertEquals(false, result.userMessage?.contains(secret) ?: false)
    }

    @Test fun nonBlankSuccessfulResponseIsSuccessRegardlessOfLatency() {
        val result = classifyProviderFailure(200, "response body", null, contentBlank = false)
        assertEquals(ProviderFailureKind.SUCCESS, result.kind)
        assertEquals(0, result.maxRetries)
        assertEquals(false, result.fallback)
    }
}
