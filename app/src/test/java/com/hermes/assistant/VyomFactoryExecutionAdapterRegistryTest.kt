package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryExecutionAdapterRegistryTest {
    @Test fun routesOnlyToAdapterMatchingExplicitTarget() {
        val android = RecordingAdapter(VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION)
        val repository = RecordingAdapter(VyomFactoryExecutionEnvelope.Target.REPOSITORY_CHANGE)
        val registry = VyomFactoryExecutionAdapterRegistry(listOf(android, repository))
        val envelope = executable("android_action", "open_settings")

        val result = registry.dispatch(envelope)

        assertEquals(VyomFactoryExecutionAdapterRegistry.Outcome.Executed("android_action:open_settings"), result)
        assertEquals(1, android.calls)
        assertEquals(0, repository.calls)
    }

    @Test fun manualReviewNeverInvokesAnAdapter() {
        val adapter = RecordingAdapter(VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION)
        val registry = VyomFactoryExecutionAdapterRegistry(listOf(adapter))

        val result = registry.dispatch(VyomFactoryExecutionEnvelope.parse("legacy request"))

        assertTrue(result is VyomFactoryExecutionAdapterRegistry.Outcome.ManualReview)
        assertEquals(0, adapter.calls)
    }

    @Test fun missingAdapterReturnsUnsupportedWithoutFallback() {
        val adapter = RecordingAdapter(VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION)
        val registry = VyomFactoryExecutionAdapterRegistry(listOf(adapter))
        val envelope = executable("repository_change", "change_with_tests")

        val result = registry.dispatch(envelope)

        assertEquals(
            VyomFactoryExecutionAdapterRegistry.Outcome.Unsupported(
                VyomFactoryExecutionEnvelope.Target.REPOSITORY_CHANGE
            ),
            result
        )
        assertEquals(0, adapter.calls)
    }

    @Test fun duplicateTargetAdaptersAreRejected() {
        val first = RecordingAdapter(VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION)
        val second = RecordingAdapter(VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION)

        val error = runCatching {
            VyomFactoryExecutionAdapterRegistry(listOf(first, second))
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test fun adapterFailureIsNotConvertedIntoSuccess() {
        val adapter = RecordingAdapter(
            VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION,
            failure = IllegalStateException("permission missing")
        )
        val registry = VyomFactoryExecutionAdapterRegistry(listOf(adapter))

        val result = runCatching {
            registry.dispatch(executable("android_action", "open_settings"))
        }

        assertFalse(result.isSuccess)
        assertEquals("permission missing", result.exceptionOrNull()?.message)
    }

    private fun executable(target: String, operation: String): VyomFactoryExecutionEnvelope {
        val operationField = if (target == "android_action") "action" else "operation"
        return VyomFactoryExecutionEnvelope.parse(
            """<!-- VYOM_FACTORY_EXECUTION:{"schema":1,"target":"$target","$operationField":"$operation","request":"do it","arguments":{}} -->"""
        )
    }

    private class RecordingAdapter(
        override val target: VyomFactoryExecutionEnvelope.Target,
        private val failure: Exception? = null
    ) : VyomFactoryExecutionAdapter {
        var calls = 0
        override fun execute(envelope: VyomFactoryExecutionEnvelope): String {
            calls++
            failure?.let { throw it }
            return "${target.name.lowercase()}: ${envelope.operation}"
        }
    }
}
