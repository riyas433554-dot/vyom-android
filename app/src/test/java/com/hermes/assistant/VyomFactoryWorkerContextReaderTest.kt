package com.hermes.assistant

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryWorkerContextReaderTest {
    private fun skill() = VyomFactoryWorkerSkillContext(
        repository = "owner/repo",
        commitSha = "a".repeat(40),
        markdown = "# Reviewed skill\n\nUse focused tests."
    )

    @Test fun skillsDisabledByDefaultAndDiskProviderIsNotInvoked() {
        var diskReads = 0
        val reader = VyomFactoryWorkerContextReader(
            memoryFactsProvider = { emptyList() },
            skillsRoot = File("unused-skills-root"),
            installedSkillsProvider = {
                diskReads++
                listOf(skill())
            }
        )
        val result = reader.compose("task")
        assertTrue(result.contains("REFERENCE CONTEXT ONLY"))
        assertFalse(result.contains("Reviewed skill"))
        assertTrue(diskReads == 0)
    }

    @Test fun enabledSkillsAreComposedThroughTheExistingReaderPath() {
        var diskReads = 0
        val reader = VyomFactoryWorkerContextReader(
            memoryFactsProvider = { emptyList() },
            skillsRoot = File("unused-skills-root"),
            skillsEnabled = true,
            installedSkillsProvider = {
                diskReads++
                listOf(skill())
            }
        )
        val result = reader.compose("task")
        assertTrue(result.contains("Reviewed skill"))
        assertTrue(result.contains("Use focused tests"))
        assertTrue(diskReads == 1)
    }

    @Test fun enabledReaderLoadsOnlyHashVerifiedPinnedReferenceFiles() {
        val root = createTempDir(prefix = "vyom-worker-skill-bundle-")
        try {
            val preview = VyomFactorySkillRepositoryClient.Preview(
                repository = "owner/repo",
                branch = "main",
                commitSha = "a".repeat(40),
                license = "MIT",
                skillMarkdown = "# Android Engineering\n\nUse the reference below.\n",
                sourceUrl = "https://github.com/owner/repo/blob/main/SKILL.md",
                referenceFiles = mapOf(
                    "references/testing.md" to "Write a failing test before changing behavior."
                )
            )
            VyomFactorySkillStore(root).install(preview, ownerConfirmed = true)
            val reader = VyomFactoryWorkerContextReader(
                memoryFactsProvider = { emptyList() },
                skillsRoot = root,
                skillsEnabled = true
            )

            val result = reader.compose("Kotlin Android debugging")

            assertTrue(result.contains("Android Engineering"))
            assertTrue(result.contains("REFERENCE FILE: references/testing.md"))
            assertTrue(result.contains("Write a failing test before changing behavior"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun memoryStillComposesWhenSkillsAreDisabled() {
        val reader = VyomFactoryWorkerContextReader(
            memoryFactsProvider = { listOf("User prefers evidence-backed reports.") },
            skillsRoot = File("unused-skills-root")
        )
        val result = reader.compose("reports")
        assertTrue(result.contains("evidence-backed reports"))
        assertFalse(result.contains("INSTALLED SKILL"))
    }
}
