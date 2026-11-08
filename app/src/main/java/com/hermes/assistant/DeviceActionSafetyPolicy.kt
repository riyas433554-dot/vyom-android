package com.hermes.assistant

/**
 * Centralizes the safety precedence used before executing device actions.
 * STOP CONTROL always wins, even when an action would otherwise be safe.
 */
internal object DeviceActionSafetyPolicy {
    enum class Decision { STOP_CONTROL, BLOCK, REQUIRE_CONFIRMATION, ALLOW }

    fun evaluate(
        killSwitchEnabled: Boolean,
        risk: ActionEngine.Risk,
        trustedDefault: Boolean = true
    ): Decision {
        if (killSwitchEnabled) return Decision.STOP_CONTROL
        return when (risk) {
            ActionEngine.Risk.BLOCKED -> Decision.BLOCK
            ActionEngine.Risk.CONFIRM -> if (trustedDefault) Decision.ALLOW else Decision.REQUIRE_CONFIRMATION
            ActionEngine.Risk.SAFE -> Decision.ALLOW
        }
    }
}
