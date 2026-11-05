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

    @Test fun genericComplaintPrefixDoesNotInventFactoryScope() {
        val extracted = VyomFactoryComplaintPrefixPolicy.extract("complaint: memory forgot my saved project")
        try {
            VyomFactoryComplaintRecord.fromFreeText("vyom_chat", extracted!!)
            throw AssertionError("Generic complaint prefix must not create Factory scope")
        } catch (_: IllegalArgumentException) {
            // Expected: generic product complaints stay outside Factory.
        }
    }

    @Test fun factoryPrefixCannotSmuggleWhatsappOrPlayStoreRequests() {
        listOf(
            "factory: WhatsApp message send failed",
            "factory complaint: Play Store install failed"
        ).forEach { command ->
            val extracted = VyomFactoryComplaintPrefixPolicy.extract(command)!!
            try {
                VyomFactoryComplaintRecord.fromFreeText("vyom_chat", extracted)
                throw AssertionError("Out-of-scope product request was accepted: $command")
            } catch (_: IllegalArgumentException) {
                // Expected: explicit prefix does not override product-scope exclusions.
            }
        }
    }

    @Test fun factoryResetIsNotMistakenForFactoryWorkflowContext() {
        try {
            VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "Factory reset is failing")
            throw AssertionError("Android factory reset must not enter Vyom Factory")
        } catch (_: IllegalArgumentException) {
            // Expected: the word factory alone is not sufficient scope evidence.
        }
    }

    @Test fun factoryUiIssueIsRecognizedAsFactoryWork() {
        val item = VyomFactoryComplaintRecord.fromFreeText("vyom_chat", "Vyom Factory UI complaint card layout is broken")
        assertEquals("ui", item.area)
    }

    @Test fun standaloneWhatsappAndPlayStoreRequestsAreRejected() {
        listOf(
            "Play Store se app install nahi ho raha",
            "WhatsApp message send nahi ho raha"
        ).forEach { complaint ->
            try {
                VyomFactoryComplaintRecord.fromFreeText("vyom_chat", complaint)
                throw AssertionError("Out-of-scope complaint was accepted: $complaint")
            } catch (_: IllegalArgumentException) {
                // Expected: standalone product features do not enter Factory.
            }
        }
    }

    @Test fun standaloneMemoryDesignVoiceAndSkillRequestsAreRejected() {
        listOf(
            "memory forgot my saved project",
            "design layout spacing is broken",
            "voice Hinglish STT drops words",
            "skill prompt wording is incorrect",
            "assistant tone is wrong"
        ).forEach { complaint ->
            try {
                VyomFactoryComplaintRecord.fromFreeText("vyom_chat", complaint)
                throw AssertionError("Out-of-scope complaint was accepted: $complaint")
            } catch (_: IllegalArgumentException) {
                // Expected: Factory is not the general Vyom product backlog.
            }
        }
    }

    @Test fun internalTaskFailureAndDiagnosticsSourcesRemainTrusted() {
        val task = VyomFactoryComplaintRecord.fromFreeText("task_failure", "Factory worker infrastructure failure. Failure type: IOException.")
        val diagnostics = VyomFactoryComplaintRecord.fromFreeText("diagnostics", "FAIL · Factory executor postcondition missing")
        assertEquals("execution", task.area)
        assertEquals("execution", diagnostics.area)
    }

    @Test fun genericControlAndUnknownComplaintsAreRejected() {
        listOf("tap button does not work", "strange unexpected behavior").forEach { complaint ->
            try {
                VyomFactoryComplaintRecord.fromFreeText("vyom_chat", complaint)
                throw AssertionError("Non-Factory complaint was accepted: $complaint")
            } catch (_: IllegalArgumentException) {
                // Expected: only Factory workflow defects are registered.
            }
        }
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
