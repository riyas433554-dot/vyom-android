package com.hermes.assistant

import org.json.JSONObject

/**
 * Narrow foreground-only Android action adapter.
 *
 * The confirmation callback must consume a one-time approval created by an
 * explicit in-app confirmation UI. A marker stored in the GitHub issue is
 * never treated as user consent. The foreground callback must reflect the
 * owning Activity's actual resumed state.
 */
internal class VyomFactoryAndroidActionAdapter(
    private val isForeground: () -> Boolean,
    private val consumeConfirmation: (String) -> Boolean,
    private val setMediaVolume: (Int) -> String
) : VyomFactoryExecutionAdapter {
    override val target = VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION

    // Setting an absolute media-volume value is safe to repeat after a crash.
    override val isIdempotent: Boolean = true

    override fun isVerifiedResult(result: String): Boolean =
        DeviceActionExecutor.auditState(result) == "verified"

    override fun run(
        envelope: VyomFactoryExecutionEnvelope,
        control: VyomFactoryExecutionControl
    ): String {
        require(envelope.isExecutable && envelope.target == target) {
            "Only validated Android action envelopes are accepted"
        }
        require(envelope.operation == SET_MEDIA_VOLUME) {
            "Unsupported Android action: ${envelope.operation}"
        }

        val arguments = envelope.arguments
        val keys = mutableSetOf<String>()
        val iterator = arguments.keys()
        while (iterator.hasNext()) keys += iterator.next()
        require(keys == REQUIRED_ARGUMENTS) {
            "set_media_volume requires exactly percent and confirmation_id"
        }

        val rawPercent = arguments.opt("percent")
        require(rawPercent is Int && rawPercent in 0..100) {
            "percent must be an integer from 0 to 100"
        }
        val confirmationId = arguments.opt("confirmation_id")
        require(confirmationId is String && confirmationId.isNotBlank() && confirmationId.length <= 128) {
            "A valid one-time confirmation_id is required"
        }

        if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
        check(isForeground()) { "Android action requires the Vyom screen to be foregrounded" }
        if (!consumeConfirmation(confirmationId)) {
            throw SecurityException("Explicit in-app confirmation is missing, expired, or already consumed")
        }

        // Re-check both cooperative STOP and foreground state immediately before
        // the only allowed side effect.
        if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
        check(isForeground()) { "Vyom left the foreground before the action could run" }

        val result = setMediaVolume(rawPercent)
        if (!isVerifiedResult(result)) {
            throw IllegalStateException("Android media-volume postcondition was not verified: ${DeviceActionExecutor.auditState(result)}")
        }
        return result
    }

    companion object {
        private const val SET_MEDIA_VOLUME = "set_media_volume"
        private val REQUIRED_ARGUMENTS = setOf("percent", "confirmation_id")

        /** Production binding; confirmation IDs are issued only by the foreground Activity UI. */
        fun forActivity(activity: MainActivity): VyomFactoryAndroidActionAdapter =
            VyomFactoryAndroidActionAdapter(
                isForeground = activity.factoryForegroundGate::isForeground,
                consumeConfirmation = activity.factoryOneTimeApprovals::consume,
                setMediaVolume = { percent ->
                    DeviceActionExecutor.execute(
                        activity,
                        ActionEngine.Intent(
                            name = "Set media volume",
                            risk = ActionEngine.Risk.SAFE,
                            operation = ActionEngine.Operation.VOLUME_SET,
                            value = percent
                        )
                    )
                }
            )
    }
}
