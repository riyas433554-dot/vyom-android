package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomInstructionsPolicyTest {
    @Test fun trimsRulesAndPreservesMeaningfulLineBreaks() {
        assertEquals(
            "Keep answers concise.\nUse Markdown for long explanations.",
            VyomInstructionsPolicy.normalizeRules("  Keep answers concise.  \n\n Use Markdown for long explanations. ")
        )
    }

    @Test fun rejectsOversizedAndSensitiveCustomRules() {
        assertFalse(VyomInstructionsPolicy.isValidRules("x".repeat(1_001)))
        assertFalse(VyomInstructionsPolicy.isValidRules("Remember my password is example-secret"))
        assertTrue(VyomInstructionsPolicy.isValidRules("Keep answers concise."))
        assertTrue(VyomInstructionsPolicy.isValidRules(""))
    }

    @Test fun normalizedRulesStayWithinThePromptBudget() {
        assertTrue(VyomInstructionsPolicy.normalizeRules("a ".repeat(700)).length <= 1_000)
    }
}
