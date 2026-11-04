package com.hermes.assistant

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class VyomFactoryAndroidActionAdapterTest {
    @Test fun acceptsOnlyConfirmedAbsoluteMediaVolumeWithVerifiedReadback() {
        var requested = -1
        val adapter = adapter(
            foreground = true,
            consume = { it == "approval-1" },
            setVolume = { requested = it; "Verified: Media volume is 5/15 (requested 35%)." }
        )

        val result = adapter.run(envelope(), NeverStop)

        assertEquals(35, requested)
        assertTrue(adapter.isVerifiedResult(result))
    }

    @Test fun rejectsUnknownAndroidOperationBeforeSideEffect() {
        var calls = 0
        val error = runCatching {
            adapter(setVolume = { calls++; "Verified: ok" }).run(
                envelope(operation = "open_settings"), NeverStop
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(0, calls)
    }

    @Test fun rejectsExtraOrMissingArguments() {
        val extra = runCatching {
            adapter().run(envelope(arguments = JSONObject().put("percent", 35).put("confirmation_id", "approval-1").put("shell", "no")), NeverStop)
        }.exceptionOrNull()
        val missing = runCatching {
            adapter().run(envelope(arguments = JSONObject().put("percent", 35)), NeverStop)
        }.exceptionOrNull()

        assertTrue(extra is IllegalArgumentException)
        assertTrue(missing is IllegalArgumentException)
    }

    @Test fun rejectsOutOfRangeAndNonIntegerPercent() {
        val tooHigh = runCatching {
            adapter().run(envelope(arguments = args(101)), NeverStop)
        }.exceptionOrNull()
        val stringValue = runCatching {
            adapter().run(envelope(arguments = JSONObject().put("percent", "35").put("confirmation_id", "approval-1")), NeverStop)
        }.exceptionOrNull()

        assertTrue(tooHigh is IllegalArgumentException)
        assertTrue(stringValue is IllegalArgumentException)
    }

    @Test fun requiresForegroundAndDoesNotConsumeApprovalInBackground() {
        var consumed = 0
        var executed = 0
        val adapter = adapter(
            foreground = false,
            consume = { consumed++; true },
            setVolume = { executed++; "Verified: ok" }
        )

        val error = runCatching { adapter.run(envelope(), NeverStop) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals(0, consumed)
        assertEquals(0, executed)
    }

    @Test fun requiresOneTimeUiApproval() {
        var executed = 0
        val adapter = adapter(consume = { false }, setVolume = { executed++; "Verified: ok" })

        val error = runCatching { adapter.run(envelope(), NeverStop) }.exceptionOrNull()

        assertTrue(error is SecurityException)
        assertEquals(0, executed)
    }

    @Test fun stopSignalIsCheckedBeforeSideEffect() {
        var executed = 0
        val adapter = adapter(setVolume = { executed++; "Verified: ok" })

        val error = runCatching { adapter.run(envelope(), StopNow) }.exceptionOrNull()

        assertTrue(error is VyomFactoryExecutionStoppedException)
        assertEquals(0, executed)
    }


    @Test fun rejectsActivityThatPausedAndResumedBeforeAdapterStarts() {
        var consumed = 0
        var executed = 0
        val adapter = adapter(
            foregroundSessionValid = false,
            consume = { consumed++; true },
            setVolume = { executed++; "Verified: ok" }
        )

        val error = runCatching { adapter.run(envelope(), NeverStop) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals(0, consumed)
        assertEquals(0, executed)
    }

    @Test fun rechecksActivitySessionAfterApprovalImmediatelyBeforeSideEffect() {
        var sessionValid = true
        var executed = 0
        val adapter = adapter(
            foregroundSessionValid = { sessionValid },
            consume = { sessionValid = false; true },
            setVolume = { executed++; "Verified: ok" }
        )

        val error = runCatching { adapter.run(envelope(), NeverStop) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals(0, executed)
    }

    @Test fun pendingOrFailedReadbackIsNeverVerified() {
        val adapter = adapter()
        assertFalse(adapter.isVerifiedResult("Pending verification: volume changed"))
        assertFalse(adapter.isVerifiedResult("Not verified: requested 5, got 4"))
        assertFalse(adapter.isVerifiedResult("Failed: permission denied"))
    }

    private fun adapter(
        foreground: Boolean = true,
        consume: (String) -> Boolean = { it == "approval-1" },
        setVolume: (Int) -> String = { "Verified: Media volume set to $it%." },
        foregroundSessionValid: () -> Boolean = { true }
    ) = VyomFactoryAndroidActionAdapter(
        isForeground = { foreground },
        isForegroundSessionValid = foregroundSessionValid,
        consumeConfirmation = consume,
        setMediaVolume = setVolume
    )

    private fun envelope(
        operation: String = "set_media_volume",
        arguments: JSONObject = args(35)
    ) = VyomFactoryExecutionEnvelope(
        target = VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION,
        originalRequest = "Set media volume to 35%",
        operation = operation,
        arguments = arguments,
        isExecutable = true
    )

    private fun args(percent: Int) = JSONObject()
        .put("percent", percent)
        .put("confirmation_id", "approval-1")

    private object NeverStop : VyomFactoryExecutionControl { override fun shouldStop() = false }
    private object StopNow : VyomFactoryExecutionControl { override fun shouldStop() = true }
}
