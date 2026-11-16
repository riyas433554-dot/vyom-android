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
