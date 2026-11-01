package com.hermes.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppAccessPolicyTest {
    @Test fun masterOffOverridesAnEnabledPackage() {
        assertFalse(AppAccessPolicy.isAllowed(false, setOf("com.example.app"), "com.example.app"))
    }

    @Test fun enabledPackageIsAllowedOnlyWhenMasterIsOn() {
        assertTrue(AppAccessPolicy.isAllowed(true, setOf("com.example.app"), "com.example.app"))
        assertFalse(AppAccessPolicy.isAllowed(true, emptySet(), "com.example.app"))
    }

    @Test fun unknownOrBlankPackagesAreDenied() {
        assertFalse(AppAccessPolicy.isAllowed(true, setOf("com.example.app"), "com.other.app"))
        assertFalse(AppAccessPolicy.isAllowed(true, setOf("com.example.app"), ""))
    }
}
