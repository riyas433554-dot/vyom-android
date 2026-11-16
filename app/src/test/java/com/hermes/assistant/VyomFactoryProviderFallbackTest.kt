package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class VyomFactoryProviderFallbackTest {
    private class KeyStore(private val key: String) : VyomFactoryWorkerApiKeyStore {
        override fun readKey() = key.toCharArray()
        override fun saveKey(value: CharArray) = Unit
        override fun clear() = Unit
    }

    private class Provider(
        override val providerId: String,
        private val response: () -> VyomFactoryWorkerModelProvider.Response
    ) : VyomFactoryWorkerModelProvider {
        var calls = 0
        override fun generate(
            request: VyomFactoryWorkerModelProvider.Request,
            apiKey: CharArray
        ): VyomFactoryWorkerModelProvider.Response {
            calls++
            return response()
        }
    }

    private fun worker(
        first: VyomFactoryWorkerModelProvider,
        fallback: List<VyomFactoryWorkerProviderRoute>,
        journal: VyomFactoryPromptInjectionTestSupport.MemoryJournal =
            VyomFactoryPromptInjectionTestSupport.MemoryJournal(),
        retries: Int = 2
    ): VyomFactoryStructuredWorkerModelAdapter {
        val primaryKeyStore = KeyStore("first-key")
        return VyomFactoryStructuredWorkerModelAdapter(
            provider = VyomFactoryFallbackWorkerModelProvider(first, fallback, retries),
            keyStore = primaryKeyStore,
            journal = journal,
            limits = VyomFactoryWorkerModelLimits(maxRetries = retries),
            preCallAuthorization = { true },
            repositoryClient = VyomFactoryRepositoryClient(getJson = { path ->
                val relative = path.substringAfter("/contents/").substringBefore("?ref=")
                val bytes = "base snapshot for $relative".toByteArray()
                """{"type":"file","path":"$relative","encoding":"base64","size":${bytes.size},"sha":"blob-sha","content":"${java.util.Base64.getEncoder().encodeToString(bytes)}"}"""
            })
        )
    }

    private fun control() = object : VyomFactoryExecutionControl {
        override fun shouldStop() = false
    }

    @Test fun modelUnavailableFallsBackImmediatelyAndPreservesNemotronFirstOrder() {
        val first = Provider("nvidia-nemotron") {
            throw VyomFactoryWorkerProviderHttpException("nvidia-nemotron", 404, "model missing")
        }
        val second = Provider("google-gemma") {
            VyomFactoryWorkerModelProvider.Response(
                VyomFactoryPromptInjectionTestSupport.validJson(), 12, 8
            )
        }
        val journal = VyomFactoryPromptInjectionTestSupport.MemoryJournal()

        val result = worker(
            first,
            listOf(VyomFactoryWorkerProviderRoute(second, KeyStore("second-key"))),
            journal
        ).propose(
            VyomFactoryPromptInjectionTestSupport.task(),
            VyomFactoryPromptInjectionTestSupport.registry(),
            control()
        )

        assertEquals("worker", result.plan.systemId)
        assertEquals(1, first.calls)
        assertEquals(1, second.calls)
        assertEquals("nvidia-nemotron", journal.entries.single().providerId)
        assertEquals("SUCCESS", journal.entries.single().result)
    }

    @Test fun transientFailureUsesBoundedRetriesBeforeFallback() {
        val first = Provider("nvidia-nemotron") {
            throw VyomFactoryWorkerProviderHttpException("nvidia-nemotron", 503, "busy")
        }
        val second = Provider("google-gemma") {
            VyomFactoryWorkerModelProvider.Response(
                VyomFactoryPromptInjectionTestSupport.validJson(), 12, 8
            )
        }

        worker(first, listOf(VyomFactoryWorkerProviderRoute(second, KeyStore("second-key"))))
            .propose(VyomFactoryPromptInjectionTestSupport.task(), VyomFactoryPromptInjectionTestSupport.registry(), control())

        assertEquals(3, first.calls)
        assertEquals(1, second.calls)
    }

    @Test fun badRequestDoesNotRetryOrFallback() {
        val first = Provider("nvidia-nemotron") {
            throw VyomFactoryWorkerProviderHttpException("nvidia-nemotron", 400, "bad payload")
        }
        val second = Provider("google-gemma") {
            VyomFactoryWorkerModelProvider.Response(
                VyomFactoryPromptInjectionTestSupport.validJson(), 12, 8
            )
        }

        assertThrows(IllegalStateException::class.java) {
            worker(first, listOf(VyomFactoryWorkerProviderRoute(second, KeyStore("second-key"))))
                .propose(VyomFactoryPromptInjectionTestSupport.task(), VyomFactoryPromptInjectionTestSupport.registry(), control())
        }

        assertEquals(1, first.calls)
        assertEquals(0, second.calls)
    }

    @Test fun networkFailureRetriesThenFallsBack() {
        val calls = AtomicInteger()
        val first = Provider("nvidia-nemotron") {
            calls.incrementAndGet()
            throw IOException("connection reset")
        }
        val second = Provider("google-gemma") {
            VyomFactoryWorkerModelProvider.Response(
                VyomFactoryPromptInjectionTestSupport.validJson(), 12, 8
            )
        }

        worker(first, listOf(VyomFactoryWorkerProviderRoute(second, KeyStore("second-key"))))
            .propose(VyomFactoryPromptInjectionTestSupport.task(), VyomFactoryPromptInjectionTestSupport.registry(), control())

        assertEquals(3, calls.get())
        assertEquals(1, second.calls)
    }

    @Test fun firstProviderSuccessDoesNotCallFallback() {
        val first = Provider("nvidia-nemotron") {
            VyomFactoryWorkerModelProvider.Response(
                VyomFactoryPromptInjectionTestSupport.validJson(), 12, 8
            )
        }
        val second = Provider("google-gemma") {
            throw AssertionError("fallback must not run after success")
        }

        worker(first, listOf(VyomFactoryWorkerProviderRoute(second, KeyStore("second-key"))))
            .propose(VyomFactoryPromptInjectionTestSupport.task(), VyomFactoryPromptInjectionTestSupport.registry(), control())

        assertEquals(1, first.calls)
        assertEquals(0, second.calls)
    }
}
