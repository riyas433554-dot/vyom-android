package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiBundleIntegrityTest {
    @Test fun computesStableSha256ForUtf8BundleContent() {
        val bytes = "vyom".toByteArray(Charsets.UTF_8)
        assertEquals("66e7ee708f37eb01d2cabbd1da2b7fd6818e5208b39f5d7a11302210c49dd212", UiBundleIntegrity.sha256(bytes))
    }

    @Test fun onlyAllowsKnownUiAssetNames() {
        assertTrue(UiBundleIntegrity.isAllowedAssetName("index.html"))
        assertTrue(UiBundleIntegrity.isAllowedAssetName("styles.css"))
        assertTrue(UiBundleIntegrity.isAllowedAssetName("app.js"))
        assertFalse(UiBundleIntegrity.isAllowedAssetName("../policy.js"))
        assertFalse(UiBundleIntegrity.isAllowedAssetName("bridge.kt"))
        assertFalse(UiBundleIntegrity.isAllowedAssetName("manifest.json"))
    }

    @Test fun rejectsHtmlWithoutRestrictiveContentSecurityPolicy() {
        val unsafe = "<html><head><script src='https://evil.example/x.js'></script></head></html>".toByteArray()
        assertFalse(UiBundleIntegrity.matches("index.html", unsafe, UiBundleIntegrity.sha256(unsafe)))
    }

    @Test fun acceptsBundledStyleJavascriptWithoutDynamicExecution() {
        val safe = "(function(){document.title='Vyom'})()".toByteArray()
        assertTrue(UiBundleIntegrity.isSafeContent("app.js", safe))
    }

    @Test fun rejectsJavascriptThatCanReachRemoteOrDynamicCodeExecution() {
        val unsafe = "fetch('https://evil.example')".toByteArray()
        assertFalse(UiBundleIntegrity.matches("app.js", unsafe, UiBundleIntegrity.sha256(unsafe)))
    }

    @Test fun rejectsHashMismatch() {
        assertFalse(UiBundleIntegrity.matches("index.html", "changed".toByteArray(), "0".repeat(64)))
    }
}
