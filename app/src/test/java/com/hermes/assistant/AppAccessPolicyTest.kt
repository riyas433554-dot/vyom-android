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

    @Test fun financeAndPaymentAppsAreBlockedByPackageOrLabel() {
        assertTrue(AppAccessPolicy.isBlockedIdentifier("com.example.wallet"))
        assertTrue(AppAccessPolicy.isBlockedIdentifier("com.example.mobile", "State Bank"))
        assertTrue(AppAccessPolicy.isBlockedIdentifier("com.phonepe.app"))
        assertFalse(AppAccessPolicy.isBlockedIdentifier("com.example.videoeditor", "CapCut"))
    }

    @Test fun tradingAndInvestmentAppsAreNotOverBlocked() {
        assertFalse(AppAccessPolicy.isBlockedIdentifier("com.example.trading", "Trading"))
        assertFalse(AppAccessPolicy.isBlockedIdentifier("com.example.investment", "Investment"))
        assertFalse(AppAccessPolicy.isBlockedIdentifier("com.zerodha.app", "Zerodha"))
    }

    @Test fun authenticatorAndOtpAppsAreBlockedByPackageOrLabel() {
        assertTrue(AppAccessPolicy.isBlockedIdentifier("com.google.android.apps.authenticator2", "Google Authenticator"))
        assertTrue(AppAccessPolicy.isBlockedIdentifier("com.example.otp"))
        assertFalse(AppAccessPolicy.isBlockedIdentifier("com.example.messaging", "Messages"))
    }

    @Test fun unknownOrBlankPackagesAreDenied() {
        assertFalse(AppAccessPolicy.isAllowed(true, setOf("com.example.app"), "com.other.app"))
        assertFalse(AppAccessPolicy.isAllowed(true, setOf("com.example.app"), ""))
    }
    @Test fun firstRunAccessDefaultsToMasterOffAndNoEnabledApps() {
        val initial = AppAccessPolicy.initialSnapshot()
        assertFalse(initial.masterEnabled)
        assertTrue(initial.enabledPackages.isEmpty())
        assertFalse(AppAccessPolicy.isAllowed(initial.masterEnabled, initial.enabledPackages, "com.example.editor"))
    }


    @Test fun controllerAppIsAllowedOnlyWhenMasterControlIsOn() {
        assertTrue(AppAccessPolicy.isAllowed(true, emptySet(), "com.vyom.assistant", "com.vyom.assistant"))
        assertFalse(AppAccessPolicy.isAllowed(false, emptySet(), "com.vyom.assistant", "com.vyom.assistant"))
        assertFalse(AppAccessPolicy.isAllowed(true, emptySet(), "com.android.vending", "com.vyom.assistant"))
    }

}
