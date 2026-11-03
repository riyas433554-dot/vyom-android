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

    @Test fun silentInstallCopiesToShellPrivatePathAndVerifiesHashBeforeInstall() {
        val hash = "a".repeat(64)
        val path = "/storage/emulated/0/Download/VyomUpdates/vyom-update-${hash.take(12)}.apk"
        assertNull(ShizukuCommandPolicy.command("install_verified_apk", listOf(path, hash), false, "com.vyom.assistant"))
        val command = ShizukuCommandPolicy.command("install_verified_apk", listOf(path, hash), true, "com.vyom.assistant")!!
        assertEquals("sh", command.argv[0])
        assertEquals("-c", command.argv[1])
        assertTrue(command.argv[2].contains("/data/local/tmp/vyom-update-$hash.apk"))
        assertTrue(command.argv[2].contains("sha256sum"))
        assertTrue(command.argv[2].contains("pm install -r"))
        assertTrue(command.timeoutMs >= 120_000L)
    }

    @Test fun silentInstallRejectsArbitraryPathsAndMismatchedHashes() {
        val hash = "a".repeat(64)
        val path = "/storage/emulated/0/Download/VyomUpdates/vyom-update-${hash.take(12)}.apk"
        assertNull(ShizukuCommandPolicy.command("install_verified_apk", listOf("/data/local/tmp/evil.apk", hash), true, "com.vyom.assistant"))
        assertNull(ShizukuCommandPolicy.command("install_verified_apk", listOf(path + ";pm clear", hash), true, "com.vyom.assistant"))
        assertNull(ShizukuCommandPolicy.command("install_verified_apk", listOf(path, "bad-hash"), true, "com.vyom.assistant"))
        assertNull(ShizukuCommandPolicy.command("install_verified_apk", listOf(path, "b".repeat(64)), true, "com.vyom.assistant"))
        assertNull(ShizukuCommandPolicy.command("install_verified_apk", listOf(path, hash, "extra"), true, "com.vyom.assistant"))
    }

    @Test fun timeoutIsAlwaysBounded() {
        val command = ShizukuCommandPolicy.command("verify_overlay", emptyList(), false, "com.vyom.assistant")!!
        assertTrue(command.timeoutMs in 100L..8000L)
    }

    @Test fun unknownCommandIdsAndUnexpectedArgumentsAreRejected() {
        assertNull(ShizukuCommandPolicy.command("verify_overlay", listOf("extra"), false, "com.vyom.assistant"))
        assertNull(ShizukuCommandPolicy.command("unknown", emptyList(), false, "com.vyom.assistant"))
    }

    @Test fun genericUiTapIsBoundToTargetDisplayAndSafeCoordinates() {
        assertEquals(
            listOf("input", "tap", "10", "20"),
            ShizukuCommandPolicy.command(
                "ui_tap", listOf("com.example.app", "10", "20", "100", "200"),
                false, "com.vyom.assistant"
            )?.argv
        )
        assertNull(ShizukuCommandPolicy.command(
            "ui_tap", listOf("com.example.app", "-1", "20", "100", "200"),
            false, "com.vyom.assistant"
        ))
        assertNull(ShizukuCommandPolicy.command(
            "ui_tap", listOf("com.example.app", "100", "20", "100", "200"),
            false, "com.vyom.assistant"
        ))
        assertNull(ShizukuCommandPolicy.command(
            "ui_tap", listOf("com.android.systemui", "10", "20", "100", "200"),
            false, "com.vyom.assistant"
        ))
    }

    @Test fun genericUiSwipeRequiresConfirmationAndValidDisplayBounds() {
        val args = listOf("com.example.app", "10", "20", "90", "180", "450", "100", "200")
        assertNull(ShizukuCommandPolicy.command("ui_swipe", args, false, "com.vyom.assistant"))
        assertEquals(
            listOf("input", "swipe", "10", "20", "90", "180", "450"),
            ShizukuCommandPolicy.command("ui_swipe", args, true, "com.vyom.assistant")?.argv
        )
        assertNull(ShizukuCommandPolicy.command(
            "ui_swipe", args.toMutableList().also { it[4] = "201" }, true, "com.vyom.assistant"
        ))
    }

    @Test fun genericKeyEventsAreRestrictedToKnownNavigationAndEditingKeys() {
        assertEquals(
            listOf("input", "keyevent", "66"),
            ShizukuCommandPolicy.command("ui_keyevent", listOf("com.example.app", "66"), false, "com.vyom.assistant")?.argv
        )
        assertNull(ShizukuCommandPolicy.command(
            "ui_keyevent", listOf("com.example.app", "999"), false, "com.vyom.assistant"
        ))
    }

    @Test fun genericTextInputRejectsCredentialPatternsAndEncodesSpaces() {
        assertEquals(
            listOf("input", "text", "hello%sworld"),
            ShizukuCommandPolicy.command("ui_type_text", listOf("com.example.app", "hello world"), false, "com.vyom.assistant")?.argv
        )
        assertNull(ShizukuCommandPolicy.command(
            "ui_type_text", listOf("com.example.app", "123456"), false, "com.vyom.assistant"
        ))
        assertNull(ShizukuCommandPolicy.command(
            "ui_type_text", listOf("com.example.app", "hello\nworld"), false, "com.vyom.assistant"
        ))
    }

}
