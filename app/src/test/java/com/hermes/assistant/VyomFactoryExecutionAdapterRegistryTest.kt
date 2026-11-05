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

        val result = registry.dispatch(envelope, NeverStop)

        assertEquals(VyomFactoryExecutionAdapterRegistry.Outcome.Executed("android_action: open_settings"), result)
        assertEquals(1, android.calls)
        assertEquals(0, repository.calls)
    }

    @Test fun manualReviewNeverInvokesAnAdapter() {
        val adapter = RecordingAdapter(VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION)
        val registry = VyomFactoryExecutionAdapterRegistry(listOf(adapter))

        val result = registry.dispatch(VyomFactoryExecutionEnvelope.parse("legacy request"), NeverStop)

        assertTrue(result is VyomFactoryExecutionAdapterRegistry.Outcome.ManualReview)
        assertEquals(0, adapter.calls)
    }

    @Test fun missingAdapterReturnsUnsupportedWithoutFallback() {
        val adapter = RecordingAdapter(VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION)
        val registry = VyomFactoryExecutionAdapterRegistry(listOf(adapter))
        val envelope = executable("repository_change", "change_with_tests")

        val result = registry.dispatch(envelope, NeverStop)

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
            registry.dispatch(executable("android_action", "open_settings"), NeverStop)
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

    @Test fun typedAdapterRejectsUnknownOperationBeforeInvocation() {
        val adapter = TypedRecordingAdapter()
        val registry = VyomFactoryExecutionAdapterRegistry(listOf(adapter))

        val result = registry.dispatch(executable("android_action", "launch_app"), NeverStop)

        assertEquals(
            VyomFactoryExecutionAdapterRegistry.Outcome.Unsupported(
                VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION
            ),
            result
        )
        assertEquals(0, adapter.calls)
    }

    @Test fun typedAdapterDispatchesOnlyItsDeclaredOperation() {
        val adapter = TypedRecordingAdapter()
        val registry = VyomFactoryExecutionAdapterRegistry(listOf(adapter))

        val result = registry.dispatch(executable("android_action", "set_media_volume"), NeverStop)

        assertEquals(
            VyomFactoryExecutionAdapterRegistry.Outcome.Executed("verified volume"),
            result
        )
        assertEquals(1, adapter.calls)
    }

    private class TypedRecordingAdapter : VyomFactoryExecutionAdapter {
        override val target = VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION
        override val isIdempotent = true
        override val capabilities = listOf(
            VyomFactoryCapabilityRegistry.Capability(
                id = "android_control.set_media_volume",
                version = 1,
                domain = VyomFactoryCapabilityRegistry.Domain.ANDROID_CONTROL,
                operations = setOf("set_media_volume"),
                risk = VyomFactoryCapabilityRegistry.Risk.R1,
                requiresOwnerApproval = false,
                mutating = true,
                reversible = false,
                rollbackCapabilityId = null,
                idempotent = true,
                available = true,
                unavailableReason = null
            )
        )
        var calls = 0
        override fun isVerifiedResult(result: String) = result == "verified volume"
        override fun run(envelope: VyomFactoryExecutionEnvelope, control: VyomFactoryExecutionControl): String {
            calls++
            return "verified volume"
        }
    }

    private object NeverStop : VyomFactoryExecutionControl { override fun shouldStop() = false }

    private class RecordingAdapter(
        override val target: VyomFactoryExecutionEnvelope.Target,
        private val failure: Exception? = null
    ) : VyomFactoryExecutionAdapter {
        override val isIdempotent: Boolean = true
        override fun isVerifiedResult(result: String) = result.startsWith("${target.name.lowercase()}:")
        var calls = 0
        override fun run(envelope: VyomFactoryExecutionEnvelope, control: VyomFactoryExecutionControl): String {
            calls++
            failure?.let { throw it }
            return "${target.name.lowercase()}: ${envelope.operation}"
        }
    }
}
