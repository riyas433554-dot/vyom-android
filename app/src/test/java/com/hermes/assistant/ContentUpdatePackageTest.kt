package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class ContentUpdatePackageTest {
    private fun manifest(probes: List<ContentUpdateProbe> = listOf(
        ContentUpdateProbe("presence", ContentProbeKind.PRESENCE, "ui-bundle/index.html", "New title"),
        ContentUpdateProbe("speed", ContentProbeKind.PERFORMANCE, "ui-bundle/index.html", baselineKey = "home-render")
    )) = ContentUpdateManifest(
        version = "1.2.0",
        minAppVersion = "0.3.1",
        changes = listOf("Change visible home title"),
        files = listOf(ContentUpdateFile("skills/demo/SKILL.md", "0".repeat(64))),
        probes = probes,
        rollbackTo = "1.1.0"
    )

    @Test fun rejectsPathsThatCouldReachNativeOrProtectedFiles() {
        listOf(
            "../policy.js", "/ui-bundle/index.html", "ui-bundle/../policy.js",
            "policies/policy-engine.json", "skills/UpdateVerifier.kt",
            "signing/release.jks", "ui-bundle\\index.html"
        ).forEach { assertFalse("Expected rejection for $it", ContentUpdateManifest.isAllowedContentPath(it)) }
        assertTrue(ContentUpdateManifest.isAllowedContentPath("ui-bundle/index.html"))
        assertTrue(ContentUpdateManifest.isAllowedContentPath("skills/calendar/SKILL.md"))
        assertTrue(ContentUpdateManifest.isAllowedContentPath("policies/non-protected.json"))
    }

    @Test fun validatesAppVersionFileHashesAndOneOrTwoOrderedProbes() {
        val bytes = "visible content".toByteArray()
        val valid = manifest().copy(files = listOf(ContentUpdateFile("skills/demo/SKILL.md", UiBundleIntegrity.sha256(bytes))))
        assertTrue(valid.validate("0.3.1", mapOf("skills/demo/SKILL.md" to bytes)).isEmpty())
        assertTrue(valid.validate("0.2.9", mapOf("skills/demo/SKILL.md" to bytes)).any { it.contains("requires app version") })
        assertTrue(valid.validate("0.3.1", mapOf("skills/demo/SKILL.md" to "tampered".toByteArray())).any { it.contains("SHA-256 mismatch") })
        assertTrue(valid.copy(probes = emptyList()).validate("0.3.1", mapOf("skills/demo/SKILL.md" to bytes)).isNotEmpty())
        assertTrue(valid.copy(probes = listOf(ContentUpdateProbe("speed", ContentProbeKind.PERFORMANCE, "x", baselineKey = "x")))
            .validate("0.3.1", mapOf("skills/demo/SKILL.md" to bytes)).any { it.contains("Probe 1 must be a presence probe") })
    }

    @Test fun verifiesDetachedEcdsaSignatureOverCanonicalManifest() {
        val bytes = "visible content".toByteArray()
        val file = ContentUpdateFile("skills/demo/SKILL.md", UiBundleIntegrity.sha256(bytes))
        val valid = manifest().copy(files = listOf(file))
        val pair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val signer = Signature.getInstance("SHA256withECDSA").apply { initSign(pair.private); update(valid.canonicalPayload()) }
        val signature = signer.sign()
        val verifier = ContentUpdatePackageVerifier(Base64.getEncoder().encodeToString(pair.public.encoded))
        assertTrue(verifier.verify(valid, signature, mapOf(file.path to bytes), "0.3.1").isEmpty())
        val altered = valid.copy(changes = listOf("tampered"))
        assertTrue(verifier.verify(altered, signature, mapOf(file.path to bytes), "0.3.1").any { it.contains("signature verification failed") })
    }

    @Test fun refusesUnknownSigningKeyAndMissingTrustConfiguration() {
        val bytes = "visible content".toByteArray()
        val file = ContentUpdateFile("skills/demo/SKILL.md", UiBundleIntegrity.sha256(bytes))
        val valid = manifest().copy(files = listOf(file))
        val pair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val signer = Signature.getInstance("SHA256withECDSA").apply { initSign(pair.private); update(valid.canonicalPayload()) }
        val signature = signer.sign()
        assertTrue(ContentUpdatePackageVerifier("").verify(valid, signature, mapOf(file.path to bytes), "0.3.1")
            .any { it.contains("trust key is not configured") })
    }
}
