package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactorySelfChangePromptTest {
    @Test fun proposalPromptCarriesIssueIdentityAndOmitsHiddenMetadata() {
        val task = task(
            "- **Verification probe:** Confirm the chat screen renders the reply\n\nDetails: WhatsApp send fails\n<!-- VYOM_FACTORY_META:private -->"
        )

        val prompt = VyomFactorySelfChangePrompt.build(task)

        assertTrue(prompt.contains("Factory complaint #42"))
        assertTrue(prompt.contains("https://github.com/riyas433554-dot/vyom-android/issues/42"))
        assertTrue(prompt.contains("WhatsApp send fails"))
        assertFalse(prompt.contains("VYOM_FACTORY_META"))
    }

    @Test fun proposalPromptRejectsSensitiveComplaintText() {
        val task = task("api_key=sk-secret-value should never be sent")
        try {
            VyomFactorySelfChangePrompt.build(task)
            fail("Sensitive complaint text must not be sent to the code model")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("Sensitive"))
        }
    }

    private fun task(body: String) = VyomFactoryGitHubClient.Task(
        number = 42,
        title = "WhatsApp messaging failure",
        body = body,
        state = "open",
        url = "https://github.com/riyas433554-dot/vyom-android/issues/42",
        updatedAt = "2026-11-04T10:00:00Z",
        metadata = VyomFactoryStatus.Metadata(
            "P0", "open", "factory", "complaint-42", "factory:complaint-42", emptyList()
        )
    )
}
