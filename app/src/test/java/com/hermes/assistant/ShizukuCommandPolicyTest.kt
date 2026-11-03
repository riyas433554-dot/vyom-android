package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class ShizukuCommandPolicyTest {
    @Test fun readBackCommandsAreFixedAndBoundToInstalledPackage() {
        assertEquals(
            listOf("pm", "check-permission", "android.permission.WRITE_SECURE_SETTINGS", "com.vyom.assistant"),
            ShizukuCommandPolicy.command("verify_secure_settings", emptyList(), false, "com.vyom.assistant")?.argv
        )
        assertEquals(
            listOf("appops", "get", "com.vyom.assistant", "SYSTEM_ALERT_WINDOW"),
            ShizukuCommandPolicy.command("verify_overlay", emptyList(), false, "com.vyom.assistant")?.argv
        )
    }

    @Test fun mutationRequiresExplicitOwnerConfirmationAndExactMode() {
        assertNull(ShizukuCommandPolicy.command("set_overlay", listOf("allow"), false, "com.vyom.assistant"))
        assertNull(ShizukuCommandPolicy.command("set_overlay", listOf("allow;pm grant"), true, "com.vyom.assistant"))
        assertEquals(
            listOf("appops", "set", "com.vyom.assistant", "SYSTEM_ALERT_WINDOW", "allow"),
            ShizukuCommandPolicy.command("set_overlay", listOf("allow"), true, "com.vyom.assistant")?.argv
        )
    }

    @Test fun arbitraryShellCommandsAndPackagesAreNeverAccepted() {
        assertNull(ShizukuCommandPolicy.command("exec", listOf("sh", "-c", "id"), true, "com.vyom.assistant"))
        assertNull(ShizukuCommandPolicy.command("verify_overlay", emptyList(), false, "com.vyom.assistant;pm"))
        assertNull(ShizukuCommandPolicy.command("grant_secure_settings", emptyList(), true, "com.vyom.assistant"))
    }

    @Test fun timeoutIsAlwaysBounded() {
        val command = ShizukuCommandPolicy.command("verify_overlay", emptyList(), false, "com.vyom.assistant")!!
        assertTrue(command.timeoutMs in 100L..8000L)
    }

    @Test fun unknownCommandIdsAndUnexpectedArgumentsAreRejected() {
        assertNull(ShizukuCommandPolicy.command("verify_overlay", listOf("extra"), false, "com.vyom.assistant"))
        assertNull(ShizukuCommandPolicy.command("unknown", emptyList(), false, "com.vyom.assistant"))
    }
}
