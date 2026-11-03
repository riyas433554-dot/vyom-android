package com.hermes.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomIdentityPolicyTest {
    @Test fun identityExplainsPurposeAndCapabilityBoundaries() {
        val prompt = VyomIdentityPolicy.systemInstruction()
        assertTrue(prompt.contains("You are Vyom"))
        assertTrue(prompt.contains("secure screens"))
        assertTrue(prompt.contains("verified"))
        assertTrue(prompt.contains("Never request or store passwords"))
        assertTrue(prompt.contains("Hermes task queue"))
        assertTrue(prompt.contains("OCR"))
    }

    @Test fun voicePromptDoesNotForceEveryReplyToOneOrTwoSentences() {
        val prompt = VyomIdentityPolicy.systemInstruction(voiceMode = true)
        assertTrue(prompt.contains("brief for simple requests, detailed when asked"))
        assertFalse(prompt.contains("Avoid lists, markdown, and long explanations"))
    }

    @Test fun identityPromptFitsTheAndroidChatContextBudget() {
        assertTrue(VyomIdentityPolicy.systemInstruction().length <= 1_400)
    }

    @Test fun languageModeIsExplicit() {
        assertTrue(VyomIdentityPolicy.systemInstruction(hinglish = true).contains("natural Hinglish"))
    }
    @Test fun ownerAuthorizedAgentLearnsBeforeGuessingAndPersistsVerifiedSkills() {
        val prompt = VyomIdentityPolicy.systemInstruction()
        assertTrue(prompt.contains("LEARN FIRST"))
        assertTrue(prompt.contains("license"))
        assertTrue(prompt.contains("verified skills"))
        assertTrue(prompt.contains("untrusted data"))
    }

    @Test fun fullControlIsPermissionAwareAndRechecksAfterActions() {
        val prompt = VyomIdentityPolicy.systemInstruction()
        assertTrue(prompt.contains("AUTHORITY"))
        assertTrue(prompt.contains("PERMISSIONS"))
        assertTrue(prompt.contains("fresh screen"))
        assertTrue(prompt.contains("STOP"))
        assertTrue(prompt.contains("safeguards"))
    }

}
