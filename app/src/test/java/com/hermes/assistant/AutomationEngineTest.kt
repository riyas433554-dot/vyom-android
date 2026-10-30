package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationEngineTest {
    private fun macro(vararg steps: AutomationEngine.Step) =
        AutomationEngine.Macro(name = "Morning routine", steps = steps.toList())

    @Test fun validatesBoundedWorkflowSteps() {
        assertTrue(AutomationEngine.validate(macro(
            AutomationEngine.Step(type = "open_app", target = "Chrome"),
            AutomationEngine.Step(type = "wait", durationMs = 500),
            AutomationEngine.Step(type = "open_url", target = "https://example.com")
        )).valid)
        assertFalse(AutomationEngine.validate(macro(AutomationEngine.Step(type = "shell", target = "rm -rf /"))).valid)
        assertFalse(AutomationEngine.validate(macro(AutomationEngine.Step(type = "wait", durationMs = 60000))).valid)
        assertFalse(AutomationEngine.validate(macro(AutomationEngine.Step(type = "open_url", target = "javascript:alert(1)"))).valid)
    }

    @Test fun safeMacrosRunWithoutConfirmation() {
        val workflow = macro(
            AutomationEngine.Step(type = "open_app", target = "Chrome"),
            AutomationEngine.Step(type = "open_url", target = "https://example.com"),
            AutomationEngine.Step(type = "scroll", target = "down"),
            AutomationEngine.Step(type = "back")
        )
        assertEquals(AutomationEngine.Risk.SAFE, AutomationEngine.classify(workflow))
        assertEquals(AutomationEngine.Decision.ALLOW, AutomationEngine.decision(workflow, ownerApproved = false))
    }

    @Test fun consequentialStepsRequireOwnerApproval() {
        val workflow = macro(
            AutomationEngine.Step(type = "open_app", target = "Chrome"),
            AutomationEngine.Step(type = "tap_text", target = "Submit"),
            AutomationEngine.Step(type = "type_text", text = "Hello")
        )
        assertEquals(AutomationEngine.Risk.CONFIRM, AutomationEngine.classify(workflow))
        assertEquals(AutomationEngine.Decision.APPROVAL_REQUIRED, AutomationEngine.decision(workflow, ownerApproved = false))
        assertEquals(AutomationEngine.Decision.ALLOW, AutomationEngine.decision(workflow, ownerApproved = true))
    }

    @Test fun sensitiveAndFinancialStepsAreAlwaysBlocked() {
        val workflows = listOf(
            macro(AutomationEngine.Step(type = "open_app", target = "Banking")),
            macro(AutomationEngine.Step(type = "tap_text", target = "Pay now")),
            macro(AutomationEngine.Step(type = "type_text", text = "OTP 123456")),
            macro(AutomationEngine.Step(type = "open_settings", target = "security"))
        )
        workflows.forEach {
            assertEquals(AutomationEngine.Risk.BLOCKED, AutomationEngine.classify(it))
            assertEquals(AutomationEngine.Decision.BLOCKED, AutomationEngine.decision(it, ownerApproved = true))
        }
    }

    @Test fun createTaskRequiresApprovalAndCredentialsCannotBeTyped() {
        val workflow = macro(AutomationEngine.Step(type = "create_task", text = "Build a new Android Jarvis app"))
        assertEquals(AutomationEngine.Risk.CONFIRM, AutomationEngine.classify(workflow))
        assertFalse(AutomationEngine.validate(macro(AutomationEngine.Step(type = "type_text", text = "password=supersecret123"))).valid)
    }

    @Test fun rejectsTooManyStepsAndInvalidGestureCoordinates() {
        val tooMany = AutomationEngine.Macro(name = "Too long", steps = List(31) { AutomationEngine.Step(type = "back") })
        assertFalse(AutomationEngine.validate(tooMany).valid)
        assertFalse(AutomationEngine.validate(macro(
            AutomationEngine.Step(type = "swipe", x1 = -0.1f, y1 = 0.2f, x2 = 0.8f, y2 = 0.9f, durationMs = 300)
        )).valid)
    }
}
