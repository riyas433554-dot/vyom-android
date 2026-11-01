package com.hermes.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomIdentityPolicyTest {
    @Test fun identityExplainsPurposeAndCapabilityBoundaries() {
        val prompt = VyomIdentityPolicy.systemInstruction()
        assertTrue(prompt.contains("You are Vyom"))
        assertTrue(prompt.contains("Android security"))
        assertTrue(prompt.contains("verified"))
        assertTrue(prompt.contains("Never ask for passwords"))
        assertTrue(prompt.contains("queued Hermes tasks"))
        assertTrue(prompt.contains("OCR"))
    }

    @Test fun voicePromptDoesNotForceEveryReplyToOneOrTwoSentences() {
        val prompt = VyomIdentityPolicy.systemInstruction(voiceMode = true)
        assertTrue(prompt.contains("Do not force every answer into one or two sentences"))
        assertFalse(prompt.contains("Avoid lists, markdown, and long explanations"))
    }

    @Test fun identityPromptFitsTheAndroidChatContextBudget() {
        assertTrue(VyomIdentityPolicy.systemInstruction().length <= 1_100)
    }

    @Test fun languageModeIsExplicit() {
        assertTrue(VyomIdentityPolicy.systemInstruction(hinglish = true).contains("natural Hinglish"))
    }
}
