package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiBundleIntegrityTest {
    @Test fun computesStableSha256ForUtf8BundleContent() {
        val bytes = "vyom".toByteArray(Charsets.UTF_8)
        assertEquals("c4c9e6d2a9a3f6b4c1c3d8f4f8c2b4b6", UiBundleIntegrity.sha256(bytes).take(32).let { it })
    }

    @Test fun onlyAllowsKnownUiAssetNames() {
        assertTrue(UiBundleIntegrity.isAllowedAssetName("index.html"))
        assertTrue(UiBundleIntegrity.isAllowedAssetName("styles.css"))
        assertTrue(UiBundleIntegrity.isAllowedAssetName("app.js"))
        assertFalse(UiBundleIntegrity.isAllowedAssetName("../policy.js"))
        assertFalse(UiBundleIntegrity.isAllowedAssetName("bridge.kt"))
        assertFalse(UiBundleIntegrity.isAllowedAssetName("manifest.json"))
    }

    @Test fun rejectsHashMismatch() {
        assertFalse(UiBundleIntegrity.matches("index.html", "changed".toByteArray(), "0".repeat(64)))
    }
}
