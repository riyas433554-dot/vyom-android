package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiAgentPolicyTest {
    @Test fun refusesToTapTextNotPresentInLatestScreen() {
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_TEXT, target = "Export"),
                listOf("Edit", "Share"), "Export video"))
    }

    @Test fun allowsOnlyExactVisibleControlTargets() {
        assertEquals(UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_TEXT, target = "Edit"),
                listOf("Edit", "Share"), "Edit this clip"))
    }

    @Test fun consequentialVisibleControlsRequireConfirmation() {
        assertEquals(UiAgentPolicy.Verdict.CONFIRM,
            UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_TEXT, target = "Delete"),
                listOf("Delete", "Cancel"), "Delete this draft"))
    }

    @Test fun refusesToTypeTextNotExplicitlyIncludedInUserGoal() {
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.TYPE_TEXT, text = "Buy premium"),
                listOf("Caption"), "Add a caption"))
    }

    @Test fun refusesCredentialLikeText() {
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.TYPE_TEXT, text = "123456"),
                listOf("Caption"), "Type 123456"))
    }

    @Test fun boundsAgentRunToTwelveActions() {
        assertTrue(UiAgentPolicy.mayContinue(0))
        assertTrue(UiAgentPolicy.mayContinue(11))
        assertFalse(UiAgentPolicy.mayContinue(12))
    }
}
