package com.hermes.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppAccessPolicyTest {
    @Test fun masterOffBlocksEveryAppEvenWhenIndividuallyEnabled() {
        val snapshot=AppAccessPolicy.Snapshot(false,setOf("com.example.editor","com.example.browser"))
        assertFalse(AppAccessPolicy.isAllowed(snapshot,"com.example.editor"))
        assertFalse(AppAccessPolicy.isAllowed(snapshot,"com.example.browser"))
    }
    @Test fun onlyIndividuallyEnabledAppsCanBeControlled() {
        val snapshot=AppAccessPolicy.Snapshot(true,setOf("com.example.editor"))
        assertTrue(AppAccessPolicy.isAllowed(snapshot,"com.example.editor"))
        assertFalse(AppAccessPolicy.isAllowed(snapshot,"com.example.browser"))
    }
    @Test fun blankAndUnknownPackagesAreDenied() {
        val snapshot=AppAccessPolicy.Snapshot(true,setOf("com.example.editor"))
        assertFalse(AppAccessPolicy.isAllowed(snapshot,""))
        assertFalse(AppAccessPolicy.isAllowed(snapshot,"com.example.other"))
    }
}