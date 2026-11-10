package com.hermes.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseIdentityPolicyTest {
    private val required = BuildConfig.REQUIRED_RELEASE_CERT_SHA256

    @Test fun acceptsOnlyTheNewFixedReleaseFingerprint() {
        assertTrue(ReleaseIdentityPolicy.matchesAny(setOf(required), required))
        assertTrue(ReleaseIdentityPolicy.matchesAny(setOf(required.uppercase()), required))
    }

    @Test fun rejectsOldAndUnrelatedSigningCertificates() {
        assertFalse(ReleaseIdentityPolicy.matchesAny(
            setOf("7a7564fca9c2a32b23dd520352a4e3aafe445621d2b99afa15407d2386699f0a"), required
        ))
        assertFalse(ReleaseIdentityPolicy.matchesAny(setOf("a".repeat(64)), required))
    }

    @Test fun rejectsMalformedRequiredFingerprint() {
        assertFalse(ReleaseIdentityPolicy.matchesAny(setOf(required), "bad"))
    }
}
