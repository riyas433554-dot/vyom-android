package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class MemoryCapturePolicyTest {
    @Test fun ordinaryChatIsNotStored() {
        assertTrue(MemoryCapturePolicy.extract("What is the latest Android version?").isEmpty())
    }

    @Test fun explicitEnglishAndHinglishRememberRequestsAreStored() {
        assertEquals(listOf("I prefer concise Hinglish answers."), MemoryCapturePolicy.extract("Please remember that I prefer concise Hinglish answers."))
        assertEquals(listOf("Mujhe short Hinglish replies pasand hain."), MemoryCapturePolicy.extract("Yaad rakhna: mujhe short Hinglish replies pasand hain."))
    }

    @Test fun preferencesAndCorrectionsAreCaptured() {
        assertEquals(listOf("I prefer dark themes."), MemoryCapturePolicy.extract("I prefer dark themes."))
        assertEquals(listOf("Keep the existing pairing flow unchanged."), MemoryCapturePolicy.extract("No, I meant keep the existing pairing flow unchanged."))
    }

    @Test fun sensitiveInformationIsRejected() {
        listOf("My OTP is 123456", "My UPI PIN is 1234", "My API key is nvapi-abcdefghijklmnop", "My bank account number is 1234567890123")
            .forEach { assertTrue(MemoryCapturePolicy.extract("Remember that $it").isEmpty()) }
        assertTrue(MemoryCapturePolicy.isSensitive("My card number is 4111111111111111"))
        assertTrue(MemoryCapturePolicy.isSensitive("My phone number is 9876543210"))
        assertTrue(MemoryCapturePolicy.isSensitive("My email is r@example.com"))
    }

    @Test fun relevantFactsAreRankedForTheCurrentMessage() {
        val facts = listOf("I like dark themes.", "Mujhe short Hinglish replies pasand hain.")
        assertEquals(listOf("Mujhe short Hinglish replies pasand hain."),
            MemoryCapturePolicy.relevantFacts(facts, "Please use short Hinglish replies"))
    }

    @Test fun safeNameStatementCanBeRemembered() {
        assertEquals(listOf("My name is Riya."), MemoryCapturePolicy.extract("My name is Riya."))
    }
}
