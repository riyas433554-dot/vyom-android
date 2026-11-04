package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class ShizukuAuditTest {
    @Test fun auditRecordsOutcomeAndExitCodeWithoutCommandOutput() {
        val event = ShizukuAudit.format("req-123", "verify_secure_settings", "success", 0)
        assertEquals("request=req-123 command=verify_secure_settings outcome=success exit=0", event)
        assertFalse(event.contains("WRITE_SECURE_SETTINGS"))
    }

    @Test fun auditRedactsUnrecognizedRequestAndCommandIdentifiers() {
        val event = ShizukuAudit.format("req-1;secret=token", "arbitrary shell text", "timeout;token", null)
        assertEquals("request=redacted command=unknown outcome=failed", event)
        assertFalse(event.contains("token"))
    }

    @Test fun nonzeroExitIsNotReportedAsSuccess() {
        val event = ShizukuAudit.format("req-2", "verify_overlay", "nonzero_exit", 13)
        assertTrue(event.contains("outcome=nonzero_exit"))
        assertTrue(event.endsWith("exit=13"))
    }
}
