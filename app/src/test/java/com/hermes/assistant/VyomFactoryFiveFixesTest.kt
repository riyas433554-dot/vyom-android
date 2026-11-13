package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VyomFactoryFiveFixesTest {
    @Test fun nestedSkillDocumentsAreDiscoverableAndRootWins() {
        assertEquals("SKILL.md", VyomFactorySkillPolicy.selectSkillDocumentPath(listOf(
            "README.md", "android/SKILL.md", "SKILL.md", "web/SKILL.md"
        )))
        assertEquals("agents/kotlin/SKILL.md", VyomFactorySkillPolicy.selectSkillDocumentPath(listOf(
            "README.md", "agents/kotlin/SKILL.md", "docs/guide.md"
        )))
    }

    @Test fun missingSkillDocumentIsRejectedClearly() {
        assertEquals(null, VyomFactorySkillPolicy.selectSkillDocumentPath(listOf("README.md", "docs/guide.md")))
    }

    @Test fun taskMetadataStateIsNotConfusedWithVerifiedImplementationProgress() {
        assertEquals("STARTED · IMPLEMENTATION NOT VERIFIED",
            VyomFactoryVerifiedProgress.taskStateLabel("in_progress", false))
        assertEquals("PAUSED · IMPLEMENTATION NOT VERIFIED",
            VyomFactoryVerifiedProgress.taskStateLabel("paused", false))
        assertEquals("IMPLEMENTATION VERIFIED",
            VyomFactoryVerifiedProgress.taskStateLabel("in_progress", true))
    }
}
