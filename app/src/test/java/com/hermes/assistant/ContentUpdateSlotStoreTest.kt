package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ContentUpdateSlotStoreTest {
    private fun manifest(version: String, rollbackTo: String, bytes: ByteArray) = ContentUpdateManifest(
        version, "0.3.1", listOf("Change UI label"),
        listOf(ContentUpdateFile("ui-bundle/index.html", UiBundleIntegrity.sha256(bytes))),
        listOf(ContentUpdateProbe("presence", ContentProbeKind.PRESENCE, "ui-bundle/index.html", "New label")),
        rollbackTo
    )

    @Test fun stagesRunsProbeAndSwitchesOnlyAfterProbePasses() {
        val root = Files.createTempDirectory("vyom-slots").toFile()
        try {
            val store = ContentUpdateSlotStore(root)
            val firstBytes = "old".toByteArray()
            val first = manifest("1.0.0", "0.9.0", firstBytes)
            assertTrue(store.applyVerified(first, mapOf("ui-bundle/index.html" to firstBytes)) { probe, slot ->
                ContentProbeResult(probe.id, File(slot, probe.target).readText() == "old", 4L, "present")
            }.passed)
            val secondBytes = "new".toByteArray()
            val second = manifest("1.1.0", "1.0.0", secondBytes)
            val result = store.applyVerified(second, mapOf("ui-bundle/index.html" to secondBytes)) { probe, slot ->
                ContentProbeResult(probe.id, File(slot, probe.target).readText() == "new", 6L, "present")
            }
            assertTrue(result.passed)
            assertEquals("1.1.0", store.activeVersion())
            assertTrue(File(root, "1.0.0/ui-bundle/index.html").readText() == "old")
        } finally { root.deleteRecursively() }
    }

    @Test fun failedProbeLeavesPreviousSlotActiveAndDeletesStagingSlot() {
        val root = Files.createTempDirectory("vyom-slots").toFile()
        try {
            val store = ContentUpdateSlotStore(root)
            val old = "old".toByteArray()
            store.applyVerified(manifest("1.0.0", "0.9.0", old), mapOf("ui-bundle/index.html" to old)) { p, _ ->
                ContentProbeResult(p.id, true, 2L, "pass")
            }
            val changed = "broken".toByteArray()
            val result = store.applyVerified(manifest("1.1.0", "1.0.0", changed), mapOf("ui-bundle/index.html" to changed)) { p, _ ->
                ContentProbeResult(p.id, false, 9L, "expected label missing")
            }
            assertFalse(result.passed)
            assertEquals("1.0.0", store.activeVersion())
            assertFalse(File(root, ".staging-1.1.0").exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun ownerRollbackRestoresPreviousSlot() {
        val root = Files.createTempDirectory("vyom-slots").toFile()
        try {
            val store = ContentUpdateSlotStore(root)
            listOf("1.0.0", "1.1.0").forEachIndexed { index, version ->
                val bytes = version.toByteArray()
                store.applyVerified(manifest(version, if (index == 0) "0.9.0" else "1.0.0", bytes),
                    mapOf("ui-bundle/index.html" to bytes)) { p, _ -> ContentProbeResult(p.id, true, 1L, "pass") }
            }
            assertTrue(store.rollback())
            assertEquals("1.0.0", store.activeVersion())
        } finally { root.deleteRecursively() }
    }

    @Test fun twoCrashesWithinSixtySecondsRollbackActiveContent() {
        val root = Files.createTempDirectory("vyom-slots").toFile()
        try {
            val store = ContentUpdateSlotStore(root)
            listOf("1.0.0", "1.1.0").forEachIndexed { index, version ->
                val bytes = version.toByteArray()
                store.applyVerified(manifest(version, if (index == 0) "0.9.0" else "1.0.0", bytes),
                    mapOf("ui-bundle/index.html" to bytes)) { p, _ -> ContentProbeResult(p.id, true, 1L, "pass") }
            }
            assertFalse(store.recordCrashAndRollback(10_000L))
            assertTrue(store.recordCrashAndRollback(40_000L))
            assertEquals("1.0.0", store.activeVersion())
        } finally { root.deleteRecursively() }
    }
}
