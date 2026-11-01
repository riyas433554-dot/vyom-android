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

    @Test fun EnglishHinglishAndBengaliParaphrasesAreRecognized() {
        assertEquals(listOf("I prefer concise replies."),
            MemoryCapturePolicy.extract("Going forward, please keep your replies concise."))
        assertEquals(listOf("Mujhe short answers pasand hain."),
            MemoryCapturePolicy.extract("Aage se mujhe short answers dena."))
        assertEquals(listOf("আমি বাংলা ভাষায় উত্তর পছন্দ করি."),
            MemoryCapturePolicy.extract("মনে রেখো, আমি বাংলা ভাষায় উত্তর পছন্দ করি।"))
        assertEquals(listOf("আমি সংক্ষিপ্ত উত্তর পছন্দ করি."),
            MemoryCapturePolicy.extract("আমি সংক্ষিপ্ত উত্তর পছন্দ করি।"))
    }

    @Test fun BengaliCorrectionsAreCapturedButTemporaryGoalsAreNot() {
        assertEquals(listOf("আমি বিস্তারিত উত্তর পছন্দ করি."),
            MemoryCapturePolicy.extract("না, আমার মানে ছিল আমি বিস্তারিত উত্তর পছন্দ করি।"))
        assertTrue(MemoryCapturePolicy.extract("আমি কালকের পরীক্ষার জন্য পড়তে চাই").isEmpty())
    }


    @Test fun BengaliSensitiveMemoryIsRejected() {
        assertTrue(MemoryCapturePolicy.extract("মনে রেখো, আমার পাসওয়ার্ড হলো example-secret").isEmpty())
        assertTrue(MemoryCapturePolicy.extract("মনে রেখো, আমার ওটিপি ১২৩৪৫৬").isEmpty())
    }


    @Test fun BengaliMemoryRetrievalUsesUnicodeTokenOverlap() {
        val facts = listOf("আমি বিস্তারিত উত্তর পছন্দ করি.", "আমি বাংলা ভাষায় উত্তর পছন্দ করি.")
        assertEquals(listOf("আমি বাংলা ভাষায় উত্তর পছন্দ করি."),
            MemoryCapturePolicy.relevantFacts(facts, "বাংলা ভাষায় উত্তর দাও", 1))
    }

}
