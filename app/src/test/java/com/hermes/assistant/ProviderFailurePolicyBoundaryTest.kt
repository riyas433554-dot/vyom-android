package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderFailurePolicyBoundaryTest {
    @Test fun retryableHttpStatusesUseBoundedRetriesAndFallback() {
        listOf(408, 425, 429, 500, 502, 503, 504).forEach { status ->
            val result = classifyProviderFailure(status, "provider response", null, false)
            assertEquals("HTTP $status kind", ProviderFailureKind.TRANSIENT, result.kind)
            assertTrue("HTTP $status retryable", result.retryable)
            assertEquals("HTTP $status retry count", 2, result.maxRetries)
            assertTrue("HTTP $status fallback", result.fallback)
        }
    }

    @Test fun unsupportedHttpStatusDoesNotEnterRetryOrFallbackLoop() {
        val result = classifyProviderFailure(501, "unsupported", null, false)
        assertEquals(ProviderFailureKind.PROVIDER_BAD_REQUEST, result.kind)
        assertFalse(result.retryable)
        assertEquals(0, result.maxRetries)
        assertFalse(result.fallback)
        assertEquals("Provider rejected the request (HTTP 501).", result.userMessage)
    }

    @Test fun httpFailureRetainsStatusWithoutRetainingResponseBody() {
        val error = VyomFactoryWorkerProviderHttpException(
            "nvidia-nemotron", 401, "sensitive provider response"
        )
        assertEquals(401, error.statusCode)
        assertFalse(error.message.orEmpty().contains("sensitive provider response"))
    }

    @Test fun successfulTwoHundredResponseWithBlankContentGetsOneRetryThenFallback() {
        val result = classifyProviderFailure(201, "  ", null, true)
        assertEquals(ProviderFailureKind.PROVIDER_EMPTY_RESPONSE, result.kind)
        assertTrue(result.retryable)
        assertEquals(1, result.maxRetries)
        assertTrue(result.fallback)
    }
}
