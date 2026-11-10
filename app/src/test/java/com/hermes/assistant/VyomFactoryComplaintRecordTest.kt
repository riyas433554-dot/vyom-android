package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryComplaintRecordTest {
    @Test fun factoryWorkerFailureIsAcceptedAsExecutionComplaint() {
        val item = VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "Factory worker lease renewal failed")
        assertEquals("execution", item.area)
        assertEquals("P0", item.priority)
    }

    @Test fun factoryTriageAndVerificationComplaintsAreAccepted() {
        val triage = VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "Vyom Factory triage routes P1 complaint incorrectly")
        val verify = VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "Factory verification probe did not record PASS")
        assertEquals("triage", triage.area)
        assertEquals("verification", verify.area)
    }

    @Test fun factoryContentUpdateAndRollbackRemainInScope() {
        val item = VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "Vyom Factory A18 signed content update rollback failed")
        assertEquals("update", item.area)
        assertEquals("P0", item.priority)
    }

    @Test fun explicitFactoryPrefixPreservesScopeForWorkerComplaints() {
        val extracted = VyomFactoryComplaintPrefixPolicy.extract("factory: worker lease renewal failed")
        assertEquals("Vyom Factory complaint: worker lease renewal failed", extracted)
        val item = VyomFactoryComplaintRecord.fromFreeText("vyom_chat", extracted!!)
        assertEquals("execution", item.area)
    }

    @Test fun genericComplaintPrefixRegistersMemoryWithoutInventingFactoryContext() {
        val extracted = VyomFactoryComplaintPrefixPolicy.extract("complaint: memory forgot my saved project")
        val item = VyomFactoryComplaintRecord.fromFreeText("vyom_chat", extracted!!)
        assertEquals("memory", item.area)
        assertEquals("P1", item.priority)
    }

    @Test fun complaintsFromEveryRequestedProductAreaAreAccepted() {
        val cases = listOf(
            "Control engine cannot tap a visible button" to "android_control",
            "Android control cannot read the current screen" to "android_control",
            "Memory forgot my saved project" to "memory",
            "Design layout spacing is broken" to "design",
            "Personality tone is wrong" to "personality",
            "Repo GitHub URL import fails" to "repo",
            "Skills cannot load the skill registry" to "skill",
            "Schedule recurring task did not run" to "schedule",
            "Voice Hinglish STT drops words" to "voice",
            "Update download verification failed" to "update",
            "WhatsApp message send failed" to "whatsapp",
            "Play Store install flow selected the wrong app" to "play_store",
            "Floating orb does not appear over another app" to "floating_orb"
        )
        cases.forEach { (complaint, expectedArea) ->
            val item = VyomFactoryComplaintRecord.fromFreeText("vyom_chat", complaint)
            assertEquals(complaint, expectedArea, item.area)
        }
    }

    @Test fun priorityPolicyKeepsOwnerTiers() {
        assertEquals("P0", VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "Control engine tap action fails").priority)
        assertEquals("P0", VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "Update installation verification failed").priority)
        assertEquals("P1", VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "Memory forgot a project").priority)
        assertEquals("P1", VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "Design layout spacing is broken").priority)
        assertEquals("P2", VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "Voice playback failed").priority)
    }

    @Test fun factoryUiIssueIsRecognizedAsFactoryWork() {
        val item = VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "Vyom Factory UI complaint card layout is broken")
        assertEquals("ui", item.area)
    }

    @Test fun factoryResetComplaintIsRegisteredAsAndroidControlInsteadOfRejected() {
        val item = VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "Factory reset flow is failing on Android")
        assertEquals("android_control", item.area)
    }

    @Test fun internalTaskFailureAndDiagnosticsSourcesRemainTrusted() {
        val task = VyomFactoryComplaintRecord.fromFreeText("task_failure", "Factory worker infrastructure failure. Failure type: IOException.")
        val diagnostics = VyomFactoryComplaintRecord.fromFreeText("diagnostics", "FAIL · Factory executor postcondition missing")
        assertEquals("execution", task.area)
        assertEquals("execution", diagnostics.area)
    }

    @Test fun externalAgentSourcesRemainDistinctInCanonicalComplaintRecords() {
        listOf("chatgpt", "claude", "codex", "termux").forEach { source ->
            val item = VyomFactoryComplaintRecord.fromFreeText(source, "Factory worker infrastructure failure")
            assertEquals(source, item.source)
            assertEquals("execution", item.area)
            assertTrue(VyomFactoryStatus.idempotencyKey(source, "agent-request-42")!!.startsWith("$source:"))
        }
    }

    @Test fun unclassifiedGenericComplaintFallsBackToOtherAndIsStillRegistered() {
        val item = VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "My Vyom feature has an unexpected problem")
        assertEquals("other", item.area)
        assertEquals("P3", item.priority)
    }

    @Test fun markdownContainsEveryRequiredRegisterFieldAndOriginalText() {
        val original = "Factory voice intake transcript is not registered in the complaint queue"
        val item = VyomFactoryComplaintRecord.fromFreeText("vyom_voice", original)
        val markdown = item.toIssueDescription()
        listOf("Source:", "Area:", "Severity:", "Priority:", "Evidence:", "Repro steps:",
            "Expected result:", "Verification probe:", "Fix type:", original).forEach {
            assertTrue("Missing field/content: $it", markdown.contains(it))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownSource() {
        VyomFactoryComplaintRecord.fromFreeText("unknown-agent", "Factory complaint register is broken")
    }
}
