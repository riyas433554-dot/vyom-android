package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceActionExecutorTest {
    @Test fun resultClassificationSeparatesVerifiedPendingAndFailedOutcomes() {
        assertEquals("verified", DeviceActionExecutor.auditState("Verified: Media volume is 6/15."))
        assertEquals("verification-pending", DeviceActionExecutor.auditState("Pending verification: Android launch request sent."))
        assertEquals("permission-required", DeviceActionExecutor.auditState("Pending permission: Grant Modify system settings."))
        assertEquals("not-verified", DeviceActionExecutor.auditState("Not verified: Android retained the previous volume."))
        assertEquals("failed", DeviceActionExecutor.auditState("Failed: No Android app can handle the request."))
    }

    @Test fun blockedAndUnperformedActionsNeverReportSuccess() {
        assertEquals("blocked", DeviceActionExecutor.auditState("Blocked: Delete is not performed automatically."))
        assertEquals("blocked", DeviceActionExecutor.auditState("Delete is not performed automatically."))
        assertEquals("blocked", DeviceActionExecutor.auditState("Only a SHA-256 and signing-certificate verified Vyom update can be installed."))
        assertEquals("executed", DeviceActionExecutor.auditState("Legacy result without a verification marker."))
    }
}
