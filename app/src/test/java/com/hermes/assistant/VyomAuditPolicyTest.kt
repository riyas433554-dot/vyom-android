package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomAuditPolicyTest {
    @Test fun sensitiveAuditValuesAreRedactedBeforePersistence() {
        assertEquals("[REDACTED]", VyomAuditPolicy.sanitize("User command: My OTP is 123456"))
        assertEquals("[REDACTED]", VyomAuditPolicy.sanitize("Install for r@example.com"))
        assertEquals("[REDACTED]", VyomAuditPolicy.sanitize("Phone 9876543210"))
        assertEquals("[REDACTED]", VyomAuditPolicy.sanitize("token sk-abcdefghijklmnop123456"))
    }

    @Test fun ordinaryAuditLabelsRemainUsefulAndBounded() {
        assertEquals("Open Wi-Fi settings", VyomAuditPolicy.sanitize("Open Wi-Fi settings"))
        assertTrue(VyomAuditPolicy.sanitize("x".repeat(500)).length <= VyomAuditPolicy.MAX_AUDIT_TEXT)
    }

    @Test fun auditValuesCannotInjectMultipleLogLines() {
        assertEquals("open settings now", VyomAuditPolicy.sanitize("open\nsettings\rnow"))
    }
}
