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

    @Test fun rejectsPaymentSecurityAndUninstallTargetsWithoutConfirmation() {
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_TEXT, target = "Security"), listOf("Security"), "Open settings"))
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_TEXT, target = "Uninstall"), listOf("Uninstall"), "Remove app"))
        val ocr = UiAgentPolicy.VisibleOcr(1, "UPI", "UPI|0|0|20|20")
        val ocrAction = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_OCR, target = "UPI", ocrIndex = 1, ocrFingerprint = ocr.fingerprint)
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(ocrAction, emptyList(), "Pay", visibleOcr = listOf(ocr)))
    }

    @Test fun refusesToTypeTextNotExplicitlyIncludedInUserGoal() {
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.TYPE_TEXT, text = "Buy premium"),
                listOf("Caption"), "Add a caption"))
    }

    @Test fun allowsExactGoalTextOnlyInObservedEditableField() {
        val fingerprint = "com.example.editor|EditText|1|2|30|40"
        val editable = UiAgentPolicy.VisibleNode(3, false, false, false, true, fingerprint, false, "Caption")
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TYPE_TEXT, text = "hello", nodeIndex = 3, nodeFingerprint = fingerprint)
        assertEquals(UiAgentPolicy.Verdict.ALLOW, UiAgentPolicy.validate(action, emptyList(), "Add caption hello", listOf(editable)))
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(action.copy(nodeIndex = 4), emptyList(), "Add caption hello", listOf(editable)))
    }

    @Test fun refusesCredentialLikeText() {
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.TYPE_TEXT, text = "123456"),
                listOf("Caption"), "Type 123456"))
    }

    @Test fun refusesNodeTapWhenIndexIsNotInLatestObservation() {
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_NODE, nodeIndex = 4, nodeFingerprint = "Button|0|0|10|10")
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(action, emptyList(), "Tap icon", emptyList()))
    }

    @Test fun refusesNonClickableOrEditableNodes() {
        val fingerprint = "Button|0|0|10|10"
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_NODE, nodeIndex = 4, nodeFingerprint = fingerprint)
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(action, emptyList(), "Tap icon", listOf(UiAgentPolicy.VisibleNode(4, false, false, false, false, fingerprint))))
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(action, emptyList(), "Tap icon", listOf(UiAgentPolicy.VisibleNode(4, true, false, false, true, fingerprint))))
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(action, emptyList(), "Tap icon", listOf(UiAgentPolicy.VisibleNode(4, true, false, true, false, fingerprint))))
    }

    @Test fun requiresConfirmationForUnlabelledClickableNode() {
        val fingerprint = "android.view.View|1|2|30|40"
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_NODE, nodeIndex = 4, nodeFingerprint = fingerprint)
        assertEquals(UiAgentPolicy.Verdict.CONFIRM, UiAgentPolicy.validate(action, emptyList(), "Tap icon", listOf(UiAgentPolicy.VisibleNode(4, true, false, false, false, fingerprint, true))))
    }

    @Test fun requiresConfirmationForLongClickableNode() {
        val fingerprint = "android.view.View|1|2|30|40"
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.LONG_PRESS_NODE, nodeIndex = 4, nodeFingerprint = fingerprint)
        assertEquals(UiAgentPolicy.Verdict.CONFIRM, UiAgentPolicy.validate(action, emptyList(), "Long press icon", listOf(UiAgentPolicy.VisibleNode(4, false, true, false, false, fingerprint, true))))
    }

    @Test fun OCRTapRequiresAnExactFreshObservedLabelAndConfirmation() {
        val observed = UiAgentPolicy.VisibleOcr(2, "Trim", "Trim|10|20|80|50")
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_OCR, target = "Trim", ocrIndex = 2, ocrFingerprint = observed.fingerprint)
        assertEquals(UiAgentPolicy.Verdict.CONFIRM, UiAgentPolicy.validate(action, emptyList(), "Trim clip", visibleOcr = listOf(observed)))
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(action.copy(ocrIndex = 3), emptyList(), "Trim clip", visibleOcr = listOf(observed)))
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(action.copy(target = "Delete"), emptyList(), "Trim clip", visibleOcr = listOf(observed)))
    }

    @Test fun onlyLaunchesAppsExplicitlyAllowedInAppAccess() {
        val allowed = setOf("com.example.editor")
        assertEquals(UiAgentPolicy.Verdict.ALLOW, UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.OPEN_APP, target = "com.example.editor"), emptyList(), "Open editor", allowedAppPackages = allowed))
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.OPEN_APP, target = "com.example.other"), emptyList(), "Open editor", allowedAppPackages = allowed))
    }

    @Test fun rejectsBlockedLabelsOnOtherwiseSafeNodeGestures() {
        val fingerprint = "android.view.View|1|2|30|40"
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_NODE, nodeIndex = 4, nodeFingerprint = fingerprint)
        val node = UiAgentPolicy.VisibleNode(4, true, false, false, false, fingerprint, true, "Payment")
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(action, emptyList(), "Pay bill", listOf(node)))
    }

    @Test fun allowsConfirmedTapOnSafeBoundedUnlabelledView() {
        val fingerprint = "android.view.View|1|2|30|40"
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_NODE, nodeIndex = 4, nodeFingerprint = fingerprint)
        val node = UiAgentPolicy.VisibleNode(4, false, false, false, false, fingerprint, true)
        assertEquals(UiAgentPolicy.Verdict.CONFIRM, UiAgentPolicy.validate(action, emptyList(), "Tap icon", listOf(node)))
    }

    @Test fun allowsOnlyDirectionalSwipeOnSafeBoundedView() {
        val fingerprint = "android.view.View|1|2|30|40"
        val node = UiAgentPolicy.VisibleNode(4, false, false, false, false, fingerprint, true)
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.SWIPE_NODE, direction = "left", nodeIndex = 4, nodeFingerprint = fingerprint)
        assertEquals(UiAgentPolicy.Verdict.CONFIRM, UiAgentPolicy.validate(action, emptyList(), "Swipe timeline", listOf(node)))
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(action.copy(direction = "diagonal"), emptyList(), "Swipe timeline", listOf(node)))
    }

    @Test fun boundsAgentRunToTwelveActions() {
        assertTrue(UiAgentPolicy.mayContinue(0))
        assertTrue(UiAgentPolicy.mayContinue(11))
        assertFalse(UiAgentPolicy.mayContinue(12))
    }
}
