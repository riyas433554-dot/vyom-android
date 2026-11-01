package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceActionSafetyPolicyTest {
    @Test fun stopControlOverridesEveryRiskLevel() {
        ActionEngine.Risk.values().forEach { risk ->
            assertEquals(DeviceActionSafetyPolicy.Decision.STOP_CONTROL,
                DeviceActionSafetyPolicy.evaluate(killSwitchEnabled = true, risk = risk))
        }
    }

    @Test fun safeActionsMayProceedWhenStopControlIsOff() {
        assertEquals(DeviceActionSafetyPolicy.Decision.ALLOW,
            DeviceActionSafetyPolicy.evaluate(killSwitchEnabled = false, risk = ActionEngine.Risk.SAFE))
    }

    @Test fun confirmActionsNeverBecomeAutomatic() {
        assertEquals(DeviceActionSafetyPolicy.Decision.REQUIRE_CONFIRMATION,
            DeviceActionSafetyPolicy.evaluate(killSwitchEnabled = false, risk = ActionEngine.Risk.CONFIRM))
    }

    @Test fun blockedActionsRemainBlocked() {
        assertEquals(DeviceActionSafetyPolicy.Decision.BLOCK,
            DeviceActionSafetyPolicy.evaluate(killSwitchEnabled = false, risk = ActionEngine.Risk.BLOCKED))
    }
}
