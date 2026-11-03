package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiBundleIntegrityTest {
    @Test fun computesStableSha256ForUtf8BundleContent() {
        val bytes = "vyom".toByteArray(Charsets.UTF_8)
        assertEquals("66e7ee708f37eb01d2cabbd1da2b7fd6818e5208b39f5d7a11302210c49dd212", UiBundleIntegrity.sha256(bytes).take(32).let { it })
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
