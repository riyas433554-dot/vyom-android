package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UiAgentPolicyTest {
    @Test fun rejectsDoneWithoutVisibleCompletionEvidence() {
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.DONE)
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action, listOf("Editing"), "Export video"))
    }

    @Test fun acceptsDoneOnlyWhenEvidenceExactlyMatchesCurrentVisibleLabel() {
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.DONE, completionEvidence = "Export complete")
        assertEquals(UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(action, listOf("Export complete", "Back"), "Export video"))
    }

    @Test fun rejectsInventedOrParaphrasedCompletionEvidence() {
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.DONE, completionEvidence = "Your video is ready")
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action, listOf("Export complete"), "Export video"))
    }

    @Test fun rejectsOverlongCompletionEvidence() {
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.DONE, completionEvidence = "x".repeat(121))
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action, listOf("x".repeat(121)), "Export video"))
    }

    @Test fun rejectsStaleScreenRevision() {
        assertFalse(UiAgentPolicy.isFreshScreen(7L, 8L, "com.example.editor", "com.example.editor"))
    }

    @Test fun rejectsForegroundAppSwitchEvenWhenRevisionMatches() {
        assertFalse(UiAgentPolicy.isFreshScreen(7L, 7L, "com.example.editor", "com.android.settings"))
    }

    @Test fun rejectsPlannerResponseWhenScreenChangesDuringPlanning() {
        assertFalse(UiAgentPolicy.isPlannerResponseFresh(12L, 13L, "com.example.editor", "com.example.editor"))
        assertFalse(UiAgentPolicy.isPlannerResponseFresh(12L, 12L, "com.example.editor", "com.android.settings"))
    }

    @Test fun acceptsPlannerResponseOnlyForSameObservedScreen() {
        assertTrue(UiAgentPolicy.isPlannerResponseFresh(12L, 12L, "com.example.editor", "com.example.editor"))
    }

    @Test fun acceptsOnlySameFreshRevisionAndForegroundPackage() {
        assertTrue(UiAgentPolicy.isFreshScreen(7L, 7L, "com.example.editor", "com.example.editor"))
        assertFalse(UiAgentPolicy.isFreshScreen(-1L, -1L, "com.example.editor", "com.example.editor"))
    }

    @Test fun stopOrGenerationChangeBlocksExecution() {
        assertFalse(UiAgentPolicy.mayExecute(false, false, 100L, null))
        assertFalse(UiAgentPolicy.mayExecute(true, true, 100L, null))
        assertTrue(UiAgentPolicy.mayExecute(true, false, 100L, null))
    }

    @Test fun sessionGrantExpiresAtBoundaryAndCannotExecute() {
        assertTrue(UiAgentPolicy.mayExecute(true, false, 99L, 100L))
        assertFalse(UiAgentPolicy.mayExecute(true, false, 100L, 100L))
        assertFalse(UiAgentPolicy.mayExecute(true, false, 101L, 100L))
    }

    @Test fun failedAndroidActionIsNeverAcceptedForProgress() {
        assertFalse(UiAgentPolicy.actionAccepted(false))
        assertTrue(UiAgentPolicy.actionAccepted(true))
    }

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
                listOf("Delete", "Cancel"), "Delete this draft", confirmationMode = UiAgentPolicy.ConfirmationMode.RISKY_ASK))
    }

    @Test fun rejectsPaymentSecurityAndUninstallTargetsWithoutConfirmation() {
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_TEXT, target = "Security"), listOf("Security"), "Open settings"))
        assertEquals(UiAgentPolicy.Verdict.ALLOW, UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_TEXT, target = "Uninstall"), listOf("Uninstall"), "Uninstall this app"))
        val ocr = UiAgentPolicy.VisibleOcr(1, "UPI", "UPI|0|0|20|20", true)
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
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(action, listOf("UPI transfer"), "Add caption hello", listOf(editable)))
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
        assertEquals(UiAgentPolicy.Verdict.CONFIRM, UiAgentPolicy.validate(action, emptyList(), "Tap icon", listOf(UiAgentPolicy.VisibleNode(4, true, false, false, false, fingerprint, true)), confirmationMode = UiAgentPolicy.ConfirmationMode.RISKY_ASK))
    }

    @Test fun requiresConfirmationForLongClickableNode() {
        val fingerprint = "android.view.View|1|2|30|40"
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.LONG_PRESS_NODE, nodeIndex = 4, nodeFingerprint = fingerprint)
        assertEquals(UiAgentPolicy.Verdict.CONFIRM, UiAgentPolicy.validate(action, emptyList(), "Long press icon", listOf(UiAgentPolicy.VisibleNode(4, false, true, false, false, fingerprint, true)), confirmationMode = UiAgentPolicy.ConfirmationMode.RISKY_ASK))
    }

    @Test fun OCRTapRequiresAnExactFreshObservedLabelAndConfirmation() {
        val observed = UiAgentPolicy.VisibleOcr(2, "Trim", "Trim|10|20|80|50", true)
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_OCR, target = "Trim", ocrIndex = 2, ocrFingerprint = observed.fingerprint)
        assertEquals(UiAgentPolicy.Verdict.CONFIRM, UiAgentPolicy.validate(action, emptyList(), "Trim clip", visibleOcr = listOf(observed), confirmationMode = UiAgentPolicy.ConfirmationMode.RISKY_ASK))
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(action.copy(ocrIndex = 3), emptyList(), "Trim clip", visibleOcr = listOf(observed)))
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(action.copy(target = "Delete"), emptyList(), "Trim clip", visibleOcr = listOf(observed)))
        val longPress = UiAgentPolicy.Action(UiAgentPolicy.Kind.LONG_PRESS_OCR, target = "Trim", ocrIndex = 2, ocrFingerprint = observed.fingerprint)
        assertEquals(UiAgentPolicy.Verdict.CONFIRM, UiAgentPolicy.validate(longPress, emptyList(), "Select clip", visibleOcr = listOf(observed), confirmationMode = UiAgentPolicy.ConfirmationMode.RISKY_ASK))
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
        assertEquals(UiAgentPolicy.Verdict.CONFIRM, UiAgentPolicy.validate(action, emptyList(), "Tap icon", listOf(node), confirmationMode = UiAgentPolicy.ConfirmationMode.RISKY_ASK))
    }

    @Test fun allowsOnlyDirectionalSwipeOnSafeBoundedView() {
        val fingerprint = "android.view.View|1|2|30|40"
        val node = UiAgentPolicy.VisibleNode(4, false, false, false, false, fingerprint, true)
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.SWIPE_NODE, direction = "left", nodeIndex = 4, nodeFingerprint = fingerprint)
        assertEquals(UiAgentPolicy.Verdict.CONFIRM, UiAgentPolicy.validate(action, emptyList(), "Swipe timeline", listOf(node), confirmationMode = UiAgentPolicy.ConfirmationMode.RISKY_ASK))
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(action.copy(direction = "diagonal"), emptyList(), "Swipe timeline", listOf(node)))
    }

    @Test fun boundsAgentRunToFortyActions() {
        assertTrue(UiAgentPolicy.mayContinue(0))
        assertTrue(UiAgentPolicy.mayContinue(39))
        assertFalse(UiAgentPolicy.mayContinue(40))
    }

    @Test fun progressCounterUsesTheFortyStepTaskBudget() {
        assertEquals("Action accepted • rechecking screen • step 1/40", UiAgentPolicy.progressMessage(1))
        assertEquals("Action accepted • rechecking screen • step 40/40", UiAgentPolicy.progressMessage(40))
    }

    @Test fun auditLabelNeverStoresTypedMessageContents() {
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TYPE_TEXT, text = "private caption 123", nodeIndex = 7)
        val label = UiAgentPolicy.auditLabel(action)
        assertTrue(label.contains("TYPE_TEXT"))
        assertTrue(label.contains("#7"))
        assertFalse(label.contains("private caption"))
        assertFalse(label.contains("123"))
    }

    @Test fun alwaysAskConfirmsEveryVisibleTextTap() {
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_TEXT, target = "Edit")
        assertEquals(UiAgentPolicy.Verdict.CONFIRM,
            UiAgentPolicy.validate(action, listOf("Edit"), "Edit clip",
                confirmationMode = UiAgentPolicy.ConfirmationMode.ALWAYS_ASK))
    }

    @Test fun riskyAsksAllowsSafeVisibleTextTapWithoutConfirmation() {
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_TEXT, target = "Edit")
        assertEquals(UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(action, listOf("Edit"), "Edit clip",
                confirmationMode = UiAgentPolicy.ConfirmationMode.RISKY_ASK))
    }

    @Test fun sessionGrantAllowsFreshBoundedNodeGestureForNonRiskyTask() {
        val fingerprint = "android.view.View|1|2|30|40"
        val node = UiAgentPolicy.VisibleNode(4, true, false, false, false, fingerprint, true)
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_NODE, nodeIndex = 4, nodeFingerprint = fingerprint)
        assertEquals(UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(action, emptyList(), "Trim this clip", listOf(node),
                confirmationMode = UiAgentPolicy.ConfirmationMode.SESSION_GRANT))
    }

    @Test fun sessionGrantStillConfirmsConsequentialOcrTargets() {
        val observed = UiAgentPolicy.VisibleOcr(2, "Send", "Send|10|20|80|50", true)
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_OCR, target = "Send", ocrIndex = 2, ocrFingerprint = observed.fingerprint)
        assertEquals(UiAgentPolicy.Verdict.CONFIRM,
            UiAgentPolicy.validate(action, emptyList(), "Send video to contact",
                visibleOcr = listOf(observed), confirmationMode = UiAgentPolicy.ConfirmationMode.SESSION_GRANT))
    }

    @Test fun sessionGrantNeverOverridesBlockedSensitiveTargets() {
        val observed = UiAgentPolicy.VisibleOcr(1, "UPI", "UPI|0|0|20|20", true)
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_OCR, target = "UPI", ocrIndex = 1, ocrFingerprint = observed.fingerprint)
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action, emptyList(), "Open app",
                visibleOcr = listOf(observed), confirmationMode = UiAgentPolicy.ConfirmationMode.SESSION_GRANT))
    }


    @Test fun OCRSwipeRequiresFreshExactSafeTargetAndConfirmation() {
        val observed = UiAgentPolicy.VisibleOcr(4, "Timeline", "Timeline|10|20|110|70", true)
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.SWIPE_OCR, target = "Timeline",
            direction = "left", ocrIndex = 4, ocrFingerprint = observed.fingerprint)
        assertEquals(UiAgentPolicy.Verdict.CONFIRM,
            UiAgentPolicy.validate(action, emptyList(), "Swipe timeline left", visibleOcr = listOf(observed), confirmationMode = UiAgentPolicy.ConfirmationMode.RISKY_ASK))
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action.copy(direction = "diagonal"), emptyList(), "Swipe timeline", visibleOcr = listOf(observed)))
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action.copy(ocrFingerprint = "stale"), emptyList(), "Swipe timeline", visibleOcr = listOf(observed)))
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action, emptyList(), "Swipe timeline",
                visibleOcr = listOf(observed.copy(gestureBoundsSafe = false))))
    }

    @Test fun OCRDragRequiresFreshExactSafeTargetAndDifferentValidAnchors() {
        val observed = UiAgentPolicy.VisibleOcr(5, "Clip", "Clip|10|20|110|70", true)
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.DRAG_OCR, target = "Clip",
            ocrIndex = 5, ocrFingerprint = observed.fingerprint, fromAnchor = "left", toAnchor = "right")
        assertEquals(UiAgentPolicy.Verdict.CONFIRM,
            UiAgentPolicy.validate(action, emptyList(), "Move clip", visibleOcr = listOf(observed), confirmationMode = UiAgentPolicy.ConfirmationMode.RISKY_ASK))
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action.copy(toAnchor = "left"), emptyList(), "Move clip", visibleOcr = listOf(observed)))
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action.copy(toAnchor = "outside"), emptyList(), "Move clip", visibleOcr = listOf(observed)))
    }

    @Test fun OCRSwipeAndDragNeverAllowSensitiveTargetsEvenWithSessionGrant() {
        val observed = UiAgentPolicy.VisibleOcr(6, "Payment", "Payment|10|20|110|70", true)
        val swipe = UiAgentPolicy.Action(UiAgentPolicy.Kind.SWIPE_OCR, target = "Payment",
            direction = "left", ocrIndex = 6, ocrFingerprint = observed.fingerprint)
        val drag = UiAgentPolicy.Action(UiAgentPolicy.Kind.DRAG_OCR, target = "Payment",
            ocrIndex = 6, ocrFingerprint = observed.fingerprint, fromAnchor = "left", toAnchor = "right")
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(swipe, emptyList(), "Edit clip",
            visibleOcr = listOf(observed), confirmationMode = UiAgentPolicy.ConfirmationMode.SESSION_GRANT))
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(drag, emptyList(), "Edit clip",
            visibleOcr = listOf(observed), confirmationMode = UiAgentPolicy.ConfirmationMode.SESSION_GRANT))
    }


    @Test fun OCRSwipeAndDragAlwaysRequireExplicitConfirmationEvenWithSessionGrant() {
        val observed = UiAgentPolicy.VisibleOcr(8, "Timeline", "Timeline|10|20|110|70", true)
        val swipe = UiAgentPolicy.Action(UiAgentPolicy.Kind.SWIPE_OCR, target = "Timeline",
            direction = "left", ocrIndex = 8, ocrFingerprint = observed.fingerprint)
        val drag = UiAgentPolicy.Action(UiAgentPolicy.Kind.DRAG_OCR, target = "Timeline",
            ocrIndex = 8, ocrFingerprint = observed.fingerprint, fromAnchor = "left", toAnchor = "right")
        assertEquals(UiAgentPolicy.Verdict.CONFIRM, UiAgentPolicy.validate(swipe, emptyList(), "Move timeline",
            visibleOcr = listOf(observed), confirmationMode = UiAgentPolicy.ConfirmationMode.SESSION_GRANT))
        assertEquals(UiAgentPolicy.Verdict.CONFIRM, UiAgentPolicy.validate(drag, emptyList(), "Move timeline",
            visibleOcr = listOf(observed), confirmationMode = UiAgentPolicy.ConfirmationMode.SESSION_GRANT))
    }

    @Test fun trustedIsTheDeclaredDefaultMode() {
        assertEquals(UiAgentPolicy.ConfirmationMode.TRUSTED, UiAgentPolicy.defaultConfirmationMode())
    }

    @Test fun trustedDefaultExecutesOwnerRequestedVisibleActionsWithoutExtraConfirmation() {
        assertEquals(UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_TEXT, target = "Send"),
                listOf("Send"), "Send this photo"))
        assertEquals(UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_TEXT, target = "Delete"),
                listOf("Delete"), "Delete this draft"))
    }

    @Test fun trustedDefaultAllowsFreshGesturesWithoutExtraConfirmation() {
        val observed = UiAgentPolicy.VisibleOcr(4, "Timeline", "Timeline|10|20|110|70", true)
        val swipe = UiAgentPolicy.Action(UiAgentPolicy.Kind.SWIPE_OCR, target = "Timeline",
            direction = "left", ocrIndex = 4, ocrFingerprint = observed.fingerprint)
        assertEquals(UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(swipe, emptyList(), "Swipe timeline left", visibleOcr = listOf(observed)))
        val node = UiAgentPolicy.VisibleNode(5, true, false, false, false, "timeline-node", true, "Timeline")
        val tap = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_NODE, nodeIndex = 5, nodeFingerprint = "timeline-node")
        assertEquals(UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(tap, emptyList(), "Open timeline", visibleNodes = listOf(node)))
    }

    @Test fun trustedDefaultAllowsAllowlistedQuickSettingsButKeepsHardBlocks() {
        val node = UiAgentPolicy.VisibleNode(3, true, false, false, false, "wifi-tile", true, "Wi-Fi")
        val action = UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_NODE, nodeIndex = 3,
            nodeFingerprint = "wifi-tile", screenPackage = AppAccessPolicy.SYSTEM_UI_PACKAGE)
        assertEquals(UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(action, listOf("Wi-Fi"), "Turn on Wi-Fi", visibleNodes = listOf(node)))
        listOf("Pay", "Banking", "Authenticator", "Security", "OTP").forEach { target ->
            assertEquals("Blocked target: $target", UiAgentPolicy.Verdict.REJECT,
                UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_TEXT, target = target),
                    listOf(target), "Open $target"))
        }
    }

    @Test fun rejectsAccessibilityTreeCapturedAcrossScreenTransition() {
        assertFalse(UiAgentPolicy.isTreeObservationFresh(20L, 21L, "com.example.editor", "com.example.editor"))
        assertFalse(UiAgentPolicy.isTreeObservationFresh(20L, 20L, "com.example.editor", "com.android.settings"))
        assertTrue(UiAgentPolicy.isTreeObservationFresh(20L, 20L, "com.example.editor", "com.example.editor"))
    }

    @Test fun tracksOwnAppContentEventsButIgnoresVyomOverlayEvents() {
        assertTrue(UiAgentPolicy.shouldTrackAccessibilityEvent(
            "com.example.editor", "com.vyom.assistant", 4, 4
        ))
        assertTrue(UiAgentPolicy.shouldTrackAccessibilityEvent(
            "com.vyom.assistant", "com.vyom.assistant", 7, 7
        ))
        assertFalse(UiAgentPolicy.shouldTrackAccessibilityEvent(
            "com.vyom.assistant", "com.vyom.assistant", 8, 7
        ))
        assertFalse(UiAgentPolicy.shouldTrackAccessibilityEvent(
            "", "com.vyom.assistant", 7, 7
        ))
    }

    @Test fun rejectsScreenChangesBetweenPlannerValidationAndActionDispatch() {
        assertFalse(UiAgentPolicy.isActionObservationFresh(30L, 31L, "com.example.editor", "com.example.editor"))
        assertFalse(UiAgentPolicy.isActionObservationFresh(30L, 30L, "com.example.editor", "com.android.settings"))
        assertTrue(UiAgentPolicy.isActionObservationFresh(30L, 30L, "com.example.editor", "com.example.editor"))
    }

    @Test fun scalesOcrCoordinatesFromScreenshotPixelsToDisplayPixels() {
        val mapped = UiAgentPolicy.mapOcrBoundsToScreen(
            left = 100, top = 200, right = 300, bottom = 400,
            imageWidth = 1080, imageHeight = 2400,
            screenWidth = 540, screenHeight = 1200
        )
        assertEquals(UiAgentPolicy.ScreenBounds(50, 100, 150, 200), mapped)
    }

    @Test fun rejectsOcrBoundsOutsideScreenshotOrInvalidDimensions() {
        assertNull(UiAgentPolicy.mapOcrBoundsToScreen(
            left = -1, top = 10, right = 20, bottom = 30,
            imageWidth = 100, imageHeight = 100, screenWidth = 100, screenHeight = 100
        ))
        assertNull(UiAgentPolicy.mapOcrBoundsToScreen(
            left = 10, top = 10, right = 20, bottom = 30,
            imageWidth = 0, imageHeight = 100, screenWidth = 100, screenHeight = 100
        ))
    }

    @Test fun rejectsOcrCoordinateMappingWhenScreenshotAndDisplayOrientationsDiffer() {
        assertNull(UiAgentPolicy.mapOcrBoundsToScreen(
            left = 100, top = 100, right = 300, bottom = 300,
            imageWidth = 2400, imageHeight = 1080,
            screenWidth = 1080, screenHeight = 2400
        ))
    }

    @Test fun rawGestureCoordinatesMustStayInsideTheObservedDisplay() {
        assertTrue(UiAgentPolicy.isGestureWithinDisplay(10f, 20f, 300f, 400f, 400, 800))
        assertFalse(UiAgentPolicy.isGestureWithinDisplay(-1f, 20f, 300f, 400f, 400, 800))
        assertFalse(UiAgentPolicy.isGestureWithinDisplay(10f, 20f, 400f, 400f, 400, 800))
        assertFalse(UiAgentPolicy.isGestureWithinDisplay(Float.NaN, 20f, 30f, 40f, 400, 800))
    }

    @Test fun waitActionsRequireAUsefulBoundedDelay() {
        assertEquals(UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.WAIT, waitMs = 250), emptyList(), "Wait for upload"))
        assertEquals(UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.WAIT, waitMs = 8000), emptyList(), "Wait for upload"))
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.WAIT, waitMs = 249), emptyList(), "Wait for upload"))
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(UiAgentPolicy.Action(UiAgentPolicy.Kind.WAIT, waitMs = 8001), emptyList(), "Wait for upload"))
    }

    @Test fun safeGlobalNavigationActionsAreAcceptedForFreshUiAgentPlans() {
        listOf(
            UiAgentPolicy.Kind.BACK, UiAgentPolicy.Kind.HOME, UiAgentPolicy.Kind.RECENTS,
            UiAgentPolicy.Kind.NOTIFICATIONS, UiAgentPolicy.Kind.QUICK_SETTINGS
        ).forEach { kind ->
            assertEquals(UiAgentPolicy.Verdict.ALLOW,
                UiAgentPolicy.validate(UiAgentPolicy.Action(kind), emptyList(), "Open notifications"))
        }
    }

    @Test fun waitResumeRequiresSameAppLiveGenerationAndUnexpiredGrant() {
        assertTrue(UiAgentPolicy.mayResumeAfterWait(true, false, 100L, 500L, "com.editor", "com.editor", 4L, 4L))
        assertFalse(UiAgentPolicy.mayResumeAfterWait(false, false, 100L, 500L, "com.editor", "com.editor", 4L, 4L))
        assertFalse(UiAgentPolicy.mayResumeAfterWait(true, true, 100L, 500L, "com.editor", "com.editor", 4L, 4L))
        assertFalse(UiAgentPolicy.mayResumeAfterWait(true, false, 500L, 500L, "com.editor", "com.editor", 4L, 4L))
        assertFalse(UiAgentPolicy.mayResumeAfterWait(true, false, 100L, null, "com.editor", "com.settings", 4L, 4L))
        assertFalse(UiAgentPolicy.mayResumeAfterWait(true, false, 100L, null, "", "", 4L, 4L))
        assertFalse(UiAgentPolicy.mayResumeAfterWait(true, false, 100L, null, "com.editor", "com.editor", 4L, 5L))
    }

    @Test fun openAppDispatchRequiresTheOriginalFreshScreenObservation() {
        assertFalse(UiAgentPolicy.isActionObservationFresh(70L, 71L, "com.editor", "com.editor"))
        assertFalse(UiAgentPolicy.isActionObservationFresh(70L, 70L, "com.editor", "com.launcher"))
        assertTrue(UiAgentPolicy.isActionObservationFresh(70L, 70L, "com.editor", "com.editor"))
    }


    @Test fun explicitPlayStoreInstallIntentAllowsOnlyTheExactInstallButtonOnMatchingListing() {
        val action = UiAgentPolicy.Action(
            UiAgentPolicy.Kind.TAP_TEXT,
            target = "Install",
            screenPackage = "com.android.vending"
        )
        assertEquals(UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(
                action,
                listOf("Godot Engine 4", "Godot Foundation", "Install"),
                "Install Godot 4 latest version from Play Store",
                confirmationMode = UiAgentPolicy.ConfirmationMode.USER_REQUESTED_INSTALL
            ))
    }

    @Test fun explicitPlayStoreInstallIntentRequiresConfirmationForWrongListingOrWrongButton() {
        val action = UiAgentPolicy.Action(
            UiAgentPolicy.Kind.TAP_TEXT,
            target = "Install",
            screenPackage = "com.android.vending"
        )
        assertEquals(UiAgentPolicy.Verdict.CONFIRM,
            UiAgentPolicy.validate(
                action,
                listOf("Godot Engine 3", "Godot Foundation", "Install"),
                "Install Godot 4 latest version from Play Store",
                confirmationMode = UiAgentPolicy.ConfirmationMode.USER_REQUESTED_INSTALL
            ))
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(
                action.copy(screenPackage = "com.example.fake.store"),
                listOf("Godot Engine 4", "Godot Foundation", "Install"),
                "Install Godot 4 latest version from Play Store",
                confirmationMode = UiAgentPolicy.ConfirmationMode.USER_REQUESTED_INSTALL
            ))
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(
                action.copy(target = "Buy"),
                listOf("Godot Engine 4", "Godot Foundation", "Buy"),
                "Install Godot 4 latest version from Play Store",
                confirmationMode = UiAgentPolicy.ConfirmationMode.USER_REQUESTED_INSTALL
            ))
    }

    @Test fun requestedInstallModeNeverAuthorizesUnrelatedConsequentialActions() {
        val action = UiAgentPolicy.Action(
            UiAgentPolicy.Kind.TAP_TEXT,
            target = "Delete",
            screenPackage = "com.android.vending"
        )
        assertEquals(UiAgentPolicy.Verdict.CONFIRM,
            UiAgentPolicy.validate(
                action,
                listOf("Godot Engine 4", "Godot Foundation", "Delete"),
                "Install Godot 4 from Play Store",
                confirmationMode = UiAgentPolicy.ConfirmationMode.USER_REQUESTED_INSTALL
            ))
    }


    @Test fun requestedInstallRequiresConfirmationForAmbiguousMultipleInstallButtons() {
        val action = UiAgentPolicy.Action(
            UiAgentPolicy.Kind.TAP_TEXT,
            target = "Install",
            screenPackage = "com.android.vending"
        )
        assertEquals(UiAgentPolicy.Verdict.CONFIRM,
            UiAgentPolicy.validate(
                action,
                listOf("Godot Engine 4", "Godot Foundation", "Install", "Install"),
                "Install Godot 4 latest version from Play Store",
                confirmationMode = UiAgentPolicy.ConfirmationMode.USER_REQUESTED_INSTALL
            ))
    }


    @Test fun requestedInstallModeAllowsSafeLabeledNavigationInsidePlayStoreOnly() {
        val fingerprint = "com.android.vending|android.view.View|10|20|200|80"
        val node = UiAgentPolicy.VisibleNode(
            3, true, false, false, false, fingerprint, true, "Godot Engine 4"
        )
        val action = UiAgentPolicy.Action(
            UiAgentPolicy.Kind.TAP_NODE,
            nodeIndex = 3,
            nodeFingerprint = fingerprint,
            screenPackage = "com.android.vending"
        )
        assertEquals(UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(
                action,
                listOf("Godot Engine 4", "Godot Foundation"),
                "Install Godot 4 latest version from Google Play Store",
                visibleNodes = listOf(node),
                confirmationMode = UiAgentPolicy.ConfirmationMode.USER_REQUESTED_INSTALL
            ))
        assertEquals(UiAgentPolicy.Verdict.CONFIRM,
            UiAgentPolicy.validate(
                action.copy(screenPackage = "com.example.other"),
                listOf("Godot Engine 4", "Godot Foundation"),
                "Install Godot 4 latest version from Google Play Store",
                visibleNodes = listOf(node),
                confirmationMode = UiAgentPolicy.ConfirmationMode.USER_REQUESTED_INSTALL
            ))
    }




    @Test fun systemUiOnlyExposesAllowlistedQuickSettingsLabelsAndGoalMatches() {
        assertTrue(UiAgentPolicy.isSafeSystemUiLabel("Wi-Fi, On"))
        assertTrue(UiAgentPolicy.isSafeSystemUiLabel("Bluetooth is Off"))
        assertTrue(UiAgentPolicy.isSafeSystemUiLabel("Brightness"))
        assertFalse(UiAgentPolicy.isSafeSystemUiLabel("Alice sent you a message"))
        assertFalse(UiAgentPolicy.isSafeSystemUiLabel("Delete"))
        assertTrue(UiAgentPolicy.isSystemUiTargetRequested("Wi-Fi, On", "Turn on Wi-Fi"))
        assertTrue(UiAgentPolicy.isSystemUiTargetRequested("Brightness", "Make the screen brighter"))
        assertTrue(UiAgentPolicy.isSystemUiTargetSatisfied("Wi-Fi, On", "Turn on Wi-Fi"))
        assertFalse(UiAgentPolicy.isSystemUiTargetSatisfied("Wi-Fi, Off", "Turn on Wi-Fi"))
        assertFalse(UiAgentPolicy.isSystemUiTargetRequested("Bluetooth", "Turn on Wi-Fi"))
    }

    @Test fun systemUiTileTapRequiresExactClickableNodeAndAlwaysConfirms() {
        val node = UiAgentPolicy.VisibleNode(
            index = 3, clickable = true, longClickable = false, password = false,
            editable = false, fingerprint = "tile-fp", gestureBoundsSafe = true, label = "Wi-Fi"
        )
        val action = UiAgentPolicy.Action(
            UiAgentPolicy.Kind.TAP_NODE, nodeIndex = 3, nodeFingerprint = "tile-fp",
            screenPackage = AppAccessPolicy.SYSTEM_UI_PACKAGE
        )
        assertEquals(UiAgentPolicy.Verdict.CONFIRM,
            UiAgentPolicy.validate(action, listOf("Wi-Fi"), "Turn on Wi-Fi",
                visibleNodes = listOf(node), confirmationMode = UiAgentPolicy.ConfirmationMode.SESSION_GRANT))
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action, listOf("Wi-Fi"), "Turn on Bluetooth",
                visibleNodes = listOf(node)))
        val alreadyOn = UiAgentPolicy.VisibleNode(
            index = 3, clickable = true, longClickable = false, password = false,
            editable = false, fingerprint = "tile-fp", gestureBoundsSafe = true, label = "Wi-Fi, On"
        )
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action, listOf("Wi-Fi, On"), "Turn on Wi-Fi",
                visibleNodes = listOf(alreadyOn)))
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(
                UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_TEXT, target = "Wi-Fi", screenPackage = AppAccessPolicy.SYSTEM_UI_PACKAGE),
                listOf("Wi-Fi"), "Turn on Wi-Fi"
            ))
    }

    @Test fun systemUiBrightnessSliderRequiresGoalMatchedDragAndConfirmation() {
        val node = UiAgentPolicy.VisibleNode(
            index = 5, clickable = false, longClickable = false, password = false,
            editable = false, fingerprint = "brightness-fp", gestureBoundsSafe = true, label = "Brightness"
        )
        val action = UiAgentPolicy.Action(
            UiAgentPolicy.Kind.DRAG_NODE, nodeIndex = 5, nodeFingerprint = "brightness-fp",
            screenPackage = AppAccessPolicy.SYSTEM_UI_PACKAGE, fromAnchor = "left", toAnchor = "right"
        )
        assertEquals(UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(action, listOf("Brightness"), "Make the screen brighter",
                visibleNodes = listOf(node)))
        val wrongDirection = action.copy(fromAnchor = "right", toAnchor = "left")
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(wrongDirection, listOf("Brightness"), "Make the screen brighter",
                visibleNodes = listOf(node)))
    }

    @Test fun systemUiCompletionRequiresVisibleRequestedToggleState() {
        assertTrue(UiAgentPolicy.isSystemUiCompletionVerified("Wi-Fi, On", "Turn on Wi-Fi"))
        assertTrue(UiAgentPolicy.isSystemUiCompletionVerified("Bluetooth, Off", "Turn off Bluetooth"))
        assertFalse(UiAgentPolicy.isSystemUiCompletionVerified("Wi-Fi, Off", "Turn on Wi-Fi"))
        assertFalse(UiAgentPolicy.isSystemUiCompletionVerified("Brightness", "Make the screen brighter"))
        assertTrue(UiAgentPolicy.isSystemUiGoalVerified(
            listOf("Wi-Fi, On", "Bluetooth, On"), "Turn on Wi-Fi and Bluetooth"
        ))
        assertFalse(UiAgentPolicy.isSystemUiGoalVerified(
            listOf("Wi-Fi, On"), "Turn on Wi-Fi and Bluetooth"
        ))
        assertFalse(UiAgentPolicy.isSystemUiGoalVerified(
            listOf("Wi-Fi, Off", "Bluetooth, On"), "Turn off Wi-Fi and turn on Bluetooth"
        ))
        assertTrue(UiAgentPolicy.isSystemUiGoalVerified(
            listOf("Wi-Fi, On"), "Open Quick Settings"
        ))
    }

    @Test fun systemUiRejectsVisualCoordinatesAndUnapprovedLabels() {
        val point = UiAgentPolicy.Action(
            UiAgentPolicy.Kind.VISION_TAP,
            screenPackage = AppAccessPolicy.SYSTEM_UI_PACKAGE,
            screenRevision = 4L,
            foregroundEpoch = 2L,
            x1 = 400, y1 = 800, x2 = 400, y2 = 800
        )
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(point, listOf("Wi-Fi"), "Turn on Wi-Fi",
                screenWidth = 1080, screenHeight = 2400, density = 3f))
        assertEquals(UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(
                UiAgentPolicy.Action(UiAgentPolicy.Kind.TAP_TEXT, target = "Delete", screenPackage = AppAccessPolicy.SYSTEM_UI_PACKAGE),
                listOf("Delete"), "Delete this notification"
            ))
    }

    @Test fun postActionContinuationRequiresSameForegroundPackageAndEpoch() {
        assertTrue(UiAgentPolicy.mayResumeAfterAction(
            generationMatches = true, stopControlOn = false,
            expectedPackage = "com.example.editor", currentPackage = "com.example.editor",
            expectedForegroundEpoch = 12L, currentForegroundEpoch = 12L
        ))
        assertFalse(UiAgentPolicy.mayResumeAfterAction(
            true, false, "com.example.editor", "com.android.settings", 12L, 13L
        ))
        assertFalse(UiAgentPolicy.mayResumeAfterAction(
            true, false, "com.example.editor", "com.example.editor", 12L, 14L
        ))
        assertFalse(UiAgentPolicy.mayResumeAfterAction(
            true, false, "com.example.editor", "", 12L, 12L
        ))
        assertFalse(UiAgentPolicy.mayResumeAfterAction(
            false, false, "com.example.editor", "com.example.editor", 12L, 12L
        ))
        assertFalse(UiAgentPolicy.mayResumeAfterAction(
            true, true, "com.example.editor", "com.example.editor", 12L, 12L
        ))
    }

    @Test fun openAppOutcomeRequiresTheRequestedPackageToBeForeground() {
        assertTrue(UiAgentPolicy.isOpenAppOutcomeVerified("com.example.editor", "com.example.editor"))
        assertFalse(UiAgentPolicy.isOpenAppOutcomeVerified("com.example.editor", "com.android.settings"))
        assertFalse(UiAgentPolicy.isOpenAppOutcomeVerified("", "com.example.editor"))
        assertFalse(UiAgentPolicy.isOpenAppOutcomeVerified("com.example.editor", ""))
    }

    @Test fun invalidatingGenerationCancelsOutstandingAgentCallbacks() {
        val activeGeneration = 41
        val stoppedGeneration = UiAgentPolicy.invalidateGeneration(activeGeneration)
        assertTrue(stoppedGeneration != activeGeneration)
        assertFalse(UiAgentPolicy.mayExecute(
            activeGeneration == stoppedGeneration, false, 1_000L, null
        ))
    }

    @Test fun clickSelectionAndFocusEventsInvalidateTheObservedScreenRevision() {
        assertTrue(UiAgentPolicy.isRelevantAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED))
        assertTrue(UiAgentPolicy.isRelevantAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED))
        assertTrue(UiAgentPolicy.isRelevantAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED))
        assertTrue(UiAgentPolicy.isRelevantAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED))
        assertFalse(UiAgentPolicy.isRelevantAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED))
    }

    @Test fun scrollUntilFoundRequiresAnAbsentVisibleTargetAndBoundedDirection() {
        val action = UiAgentPolicy.Action(
            UiAgentPolicy.Kind.SCROLL_UNTIL_FOUND,
            target = "Downloads",
            direction = "down",
            maxScrolls = 4
        )
        assertEquals(
            UiAgentPolicy.Verdict.ALLOW,
            UiAgentPolicy.validate(action, listOf("Settings", "Storage"), "Find Downloads")
        )
        assertEquals(
            UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action, listOf("Downloads"), "Find Downloads")
        )
        assertEquals(
            UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action.copy(direction = "diagonal"), emptyList(), "Find Downloads")
        )
        assertEquals(
            UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action.copy(maxScrolls = 0), emptyList(), "Find Downloads")
        )
        assertEquals(
            UiAgentPolicy.Verdict.REJECT,
            UiAgentPolicy.validate(action.copy(maxScrolls = 9), emptyList(), "Find Downloads")
        )
    }
}
