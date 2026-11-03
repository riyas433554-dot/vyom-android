package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class ContentUpdateSlotStoreTest {
    private val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
    private val verifier = ContentUpdatePackageVerifier(Base64.getEncoder().encodeToString(keyPair.public.encoded))

    private fun signed(manifest: ContentUpdateManifest): ByteArray =
        Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(manifest.canonicalPayload())
            sign()
        }

    private fun apply(store: ContentUpdateSlotStore, manifest: ContentUpdateManifest, bytes: ByteArray,
                      runner: ContentUpdateProbeRunner) =
        store.applySigned(manifest, signed(manifest), mapOf("skills/demo/SKILL.md" to bytes),
            "0.3.1", verifier, runner)

    private fun manifest(version: String, rollbackTo: String, bytes: ByteArray) = ContentUpdateManifest(
        version, "0.3.1", listOf("Change UI label"),
        listOf(ContentUpdateFile("skills/demo/SKILL.md", UiBundleIntegrity.sha256(bytes))),
        listOf(ContentUpdateProbe("presence", ContentProbeKind.PRESENCE, "skills/demo/SKILL.md", "New label")),
        rollbackTo
    )

    @Test fun stagesRunsProbeAndSwitchesOnlyAfterProbePasses() {
        val root = Files.createTempDirectory("vyom-slots").toFile()
        try {
            val store = ContentUpdateSlotStore(root)
            val baselineBytes = "baseline".toByteArray()
            store.seedInitialSlot("0.9.0", mapOf(
                "skills/demo/SKILL.md" to baselineBytes,
                "skills/calendar/SKILL.md" to "calendar skill v1".toByteArray()
            ))
            val firstBytes = "old".toByteArray()
            val first = manifest("1.0.0", "0.9.0", firstBytes)
            assertTrue(apply(store, first, firstBytes) { probe, slot ->
                ContentProbeResult(probe.id, File(slot, probe.target).readText() == "old", 4L, "present")
            }.passed)
            val secondBytes = "new".toByteArray()
            val second = manifest("1.1.0", "1.0.0", secondBytes)
            val result = apply(store, second, secondBytes) { probe, slot ->
                ContentProbeResult(probe.id, File(slot, probe.target).readText() == "new", 6L, "present")
            }
            assertTrue(result.passed)
            assertEquals("1.1.0", store.activeVersion())
            assertTrue(File(root, "1.0.0/skills/demo/SKILL.md").readText() == "old")
            assertEquals("calendar skill v1", File(root, "1.1.0/skills/calendar/SKILL.md").readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun failedProbeLeavesPreviousSlotActiveAndDeletesStagingSlot() {
        val root = Files.createTempDirectory("vyom-slots").toFile()
        try {
            val store = ContentUpdateSlotStore(root)
            val baseline = "baseline".toByteArray()
            store.seedInitialSlot("0.9.0", mapOf("skills/demo/SKILL.md" to baseline))
            val old = "old".toByteArray()
            apply(store, manifest("1.0.0", "0.9.0", old), old) { p, _ ->
                ContentProbeResult(p.id, true, 2L, "pass")
            }
            val changed = "broken".toByteArray()
            val result = apply(store, manifest("1.1.0", "1.0.0", changed), changed) { p, _ ->
                ContentProbeResult(p.id, false, 9L, "expected label missing")
            }
            assertFalse(result.passed)
            assertEquals("1.0.0", store.activeVersion())
            assertFalse(File(root, ".staging-1.1.0").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun rejectsOlderContentVersionWithoutChangingTheActiveSlot() {
        val root = Files.createTempDirectory("vyom-slots").toFile()
        try {
            val store = ContentUpdateSlotStore(root)
            store.seedInitialSlot("1.0.0", mapOf("skills/demo/SKILL.md" to "base".toByteArray()))
            val current = "current".toByteArray()
            assertTrue(apply(store, manifest("1.2.0", "previous", current), current) { p, _ ->
                ContentProbeResult(p.id, true, 1L, "pass")
            }.passed)
            val stale = "stale".toByteArray()
            val result = apply(store, manifest("1.1.0", "previous", stale), stale) { p, _ ->
                ContentProbeResult(p.id, true, 1L, "pass")
            }
            assertFalse(result.passed)
            assertEquals("1.2.0", store.activeVersion())
        } finally { root.deleteRecursively() }
    }

    @Test fun ownerRollbackRestoresPreviousSlot() {
        val root = Files.createTempDirectory("vyom-slots").toFile()
        try {
            val store = ContentUpdateSlotStore(root)
            store.seedInitialSlot("0.9.0", mapOf("skills/demo/SKILL.md" to "baseline".toByteArray()))
            listOf("1.0.0", "1.1.0").forEachIndexed { index, version ->
                val bytes = version.toByteArray()
                apply(store, manifest(version, if (index == 0) "0.9.0" else "1.0.0", bytes),
                    bytes) { p, _ -> ContentProbeResult(p.id, true, 1L, "pass") }
            }
            assertTrue(store.rollback())
            assertEquals("1.0.0", store.activeVersion())
        } finally { root.deleteRecursively() }
    }


    @Test fun performanceProbeReportsFasterAndSlowerDeltasAgainstStoredBaseline() {
        val root = Files.createTempDirectory("vyom-slots").toFile()
        try {
            val store = ContentUpdateSlotStore(root)
            val old = "baseline".toByteArray()
            store.seedInitialSlot("1.0.0", mapOf("skills/demo/SKILL.md" to old))
            store.recordPerformanceBaseline("home-render", 100L)
            fun performanceManifest(version: String, rollback: String, bytes: ByteArray) =
                ContentUpdateManifest(
                    version, "0.3.1", listOf("Tune home render"),
                    listOf(ContentUpdateFile("skills/demo/SKILL.md", UiBundleIntegrity.sha256(bytes))),
                    listOf(
                        ContentUpdateProbe("presence", ContentProbeKind.PRESENCE, "skills/demo/SKILL.md", "home"),
                        ContentUpdateProbe("speed", ContentProbeKind.PERFORMANCE, "skills/demo/SKILL.md", baselineKey = "home-render")
                    ), rollback
                )
            val fasterBytes = "faster".toByteArray()
            val fasterManifest = performanceManifest("1.1.0", "1.0.0", fasterBytes)
            val faster = store.applySigned(fasterManifest, signed(fasterManifest),
                mapOf("skills/demo/SKILL.md" to fasterBytes), "0.3.1", verifier) { p, _ ->
                ContentProbeResult(p.id, true, if (p.kind == ContentProbeKind.PERFORMANCE) 80L else 1L, "pass")
            }
            assertTrue(faster.passed)
            assertEquals(-20L, faster.timingDeltaMs)
            assertEquals("faster 20 ms", faster.timingSummary())
            val slowerBytes = "slower".toByteArray()
            val slowerManifest = performanceManifest("1.2.0", "1.1.0", slowerBytes)
            val slower = store.applySigned(slowerManifest, signed(slowerManifest),
                mapOf("skills/demo/SKILL.md" to slowerBytes), "0.3.1", verifier) { p, _ ->
                ContentProbeResult(p.id, true, if (p.kind == ContentProbeKind.PERFORMANCE) 125L else 1L, "pass")
            }
            assertTrue(slower.passed)
            assertEquals(45L, slower.timingDeltaMs)
            assertEquals("slower +45 ms", slower.timingSummary())
        } finally { root.deleteRecursively() }
    }


    @Test fun pageOrSkillLoadFailureAutomaticallyRestoresPreviousSlot() {
        val root = Files.createTempDirectory("vyom-slots").toFile()
        try {
            val store = ContentUpdateSlotStore(root)
            store.seedInitialSlot("0.9.0", mapOf("skills/demo/SKILL.md" to "baseline".toByteArray()))
            listOf("1.0.0", "1.1.0").forEachIndexed { index, version ->
                val bytes = version.toByteArray()
                apply(store, manifest(version, if (index == 0) "0.9.0" else "1.0.0", bytes), bytes) { p, _ ->
                    ContentProbeResult(p.id, true, 1L, "staging probe passed")
                }
            }
            assertFalse(store.rollbackAfterLoadFailure("1.0.0"))
            assertTrue(store.rollbackAfterLoadFailure("1.1.0"))
            assertEquals("1.0.0", store.activeVersion())
            assertTrue(store.history().any { it.contains("Automatic rollback after page/skill load failure") })
        } finally { root.deleteRecursively() }
    }


    @Test fun exposesOnlyFilesInsideTheActiveSlotAndDetectsStoredPerformanceBaseline() {
        val root = Files.createTempDirectory("vyom-slots").toFile()
        try {
            val store = ContentUpdateSlotStore(root)
            val bytes = "visible UI".toByteArray()
            store.seedInitialSlot("1.0.0", mapOf("ui-bundle/index.html" to bytes))
            assertEquals("visible UI", store.activeFile("ui-bundle/index.html").readText())
            assertFalse(store.hasPerformanceBaseline("home-render"))
            store.recordPerformanceBaseline("home-render", 120L)
            assertTrue(store.hasPerformanceBaseline("home-render"))
            try {
                store.activeFile("../outside")
                fail("Path traversal must not escape the active slot")
            } catch (_: IllegalArgumentException) { }
        } finally { root.deleteRecursively() }
    }


    @Test fun twoCrashesWithinSixtySecondsRollbackActiveContent() {
        val root = Files.createTempDirectory("vyom-slots").toFile()
        try {
            val store = ContentUpdateSlotStore(root)
            store.seedInitialSlot("1.0.0", mapOf("skills/demo/SKILL.md" to "baseline".toByteArray()))
            val updated = "updated".toByteArray()
            apply(store, manifest("1.1.0", "1.0.0", updated), updated) { p, _ ->
                ContentProbeResult(p.id, true, 1L, "pass")
            }
            assertEquals("1.1.0", store.activeVersion())
            assertFalse(store.recordCrashAndRollback(10_000L))
            assertTrue(store.recordCrashAndRollback(40_000L))
            assertEquals("1.0.0", store.activeVersion())
            assertTrue(store.history().any { it.contains("Automatic rollback after two crashes") })
        } finally { root.deleteRecursively() }
    }}
