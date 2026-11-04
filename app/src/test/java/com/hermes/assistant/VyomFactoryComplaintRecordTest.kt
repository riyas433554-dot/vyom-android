package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryComplaintRecordTest {
    @Test fun controlEngineComplaintGetsTopPriorityAndControlArea() {
        val item = VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "tap button does not work")
        assertEquals("control", item.area)
        assertEquals("P0", item.priority)
    }

    @Test fun memoryComplaintRanksAheadOfDesignAndOtherWork() {
        val item = VyomFactoryComplaintRecord.fromFreeText("vyom_voice", "memory forgot my saved project")
        assertEquals("memory", item.area)
        assertEquals("P1", item.priority)
    }

    @Test fun designComplaintUsesDesignPriority() {
        val item = VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "design layout spacing is broken")
        assertEquals("design", item.area)
        assertEquals("P2", item.priority)
    }

    @Test fun contentOnlySkillComplaintCanAvoidApk() {
        val item = VyomFactoryComplaintRecord.fromFreeText("chatgpt", "skill prompt wording is incorrect")
        assertEquals("skill", item.area)
        assertEquals("content update with no APK", item.fixType)
    }

    @Test fun unknownComplaintIsNotGuessedIntoAFeatureArea() {
        val item = VyomFactoryComplaintRecord.fromFreeText("termux", "strange unexpected behavior")
        assertEquals("other", item.area)
        assertEquals("P3", item.priority)
    }

    @Test fun personalityAreaRequiresAnExplicitPersonaSignal() {
        val generic = VyomFactoryComplaintRecord.fromFreeText("termux", "unexpected behavior")
        val explicit = VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "assistant tone is wrong")
        assertEquals("other", generic.area)
        assertEquals("personality", explicit.area)
    }

    @Test fun markdownContainsEveryRequiredRegisterFieldAndOriginalText() {
        val original = "voice: Hinglish STT drops words"
        val item = VyomFactoryComplaintRecord.fromFreeText("vyom_voice", original)
        val markdown = item.toIssueDescription()
        listOf("Source:", "Area:", "Severity:", "Priority:", "Evidence:", "Repro steps:",
            "Expected result:", "Verification probe:", "Fix type:", original).forEach {
            assertTrue("Missing field/content: $it", markdown.contains(it))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownSource() {
        VyomFactoryComplaintRecord.fromFreeText("unknown-agent", "complaint")
    }
}
