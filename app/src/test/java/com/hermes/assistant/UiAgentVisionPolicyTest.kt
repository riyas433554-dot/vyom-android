package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiAgentVisionPolicyTest {
    @Test fun normalizedCoordinatesMapToScreenPixels() {
        assertEquals(0, UiAgentVisionPolicy.normalizedToPixels(0, 1080))
        assertEquals(539, UiAgentVisionPolicy.normalizedToPixels(500, 1080))
        assertEquals(1079, UiAgentVisionPolicy.normalizedToPixels(1000, 1080))
    }

    @Test fun normalizedCoordinatesOutsideImageAreRejected() {
        assertEquals(null, UiAgentVisionPolicy.normalizedToPixels(-1, 1080))
        assertEquals(null, UiAgentVisionPolicy.normalizedToPixels(1001, 1080))
        assertEquals(null, UiAgentVisionPolicy.normalizedToPixels(500, 0))
    }

    @Test fun pointTapMustStayInsideSafeDisplayArea() {
        assertTrue(UiAgentVisionPolicy.isSafeGesture("tap", 540, 900, 540, 900, 1080, 2400, 3f, false))
        assertFalse(UiAgentVisionPolicy.isSafeGesture("tap", 5, 900, 5, 900, 1080, 2400, 3f, false))
        assertFalse(UiAgentVisionPolicy.isSafeGesture("tap", 1080, 900, 1080, 900, 1080, 2400, 3f, false))
    }

    @Test fun swipeAndDragRequireMeaningfulBoundedMovement() {
        assertTrue(UiAgentVisionPolicy.isSafeGesture("swipe", 200, 900, 700, 900, 1080, 2400, 3f, false))
        assertFalse(UiAgentVisionPolicy.isSafeGesture("swipe", 200, 900, 205, 900, 1080, 2400, 3f, false))
        assertFalse(UiAgentVisionPolicy.isSafeGesture("drag", 200, 900, 1070, 900, 1080, 2400, 3f, false))
    }

    @Test fun visualCoordinatesAreRejectedWhenSensitiveContextExists() {
        assertFalse(UiAgentVisionPolicy.isSafeGesture("tap", 540, 900, 540, 900, 1080, 2400, 3f, true))
    }

    @Test fun screenshotAndDisplayAspectRatiosMustMatch() {
        assertTrue(UiAgentVisionPolicy.isAspectCompatible(720, 1600, 1080, 2400))
        assertFalse(UiAgentVisionPolicy.isAspectCompatible(720, 1200, 1080, 2400))
    }

    @Test fun sensitiveTextAndPasswordFieldsBlockVisualGestures() {
        assertTrue(UiAgentVisionPolicy.hasSensitiveContext(listOf("Enter OTP"), false))
        assertTrue(UiAgentVisionPolicy.hasSensitiveContext(listOf("Delete"), false))
        assertTrue(UiAgentVisionPolicy.hasSensitiveContext(listOf("123456"), false))
        assertTrue(UiAgentVisionPolicy.hasSensitiveContext(listOf("person@example.com"), false))
        assertTrue(UiAgentVisionPolicy.hasSensitiveContext(emptyList(), true))
        assertFalse(UiAgentVisionPolicy.hasSensitiveContext(listOf("Trim clip", "Export preview"), false))
    }

    @Test fun visualActionsAlwaysRequireConfirmationEvenWithSessionGrant() {
        val action = UiAgentPolicy.Action(
            kind = UiAgentPolicy.Kind.VISION_TAP,
            screenPackage = "com.example.editor",
            screenRevision = 7L,
            foregroundEpoch = 3L,
            x1 = 540, y1 = 900, x2 = 540, y2 = 900
        )
        assertEquals(UiAgentPolicy.Verdict.CONFIRM, UiAgentPolicy.validate(
            action, emptyList(), "Trim the selected clip",
            confirmationMode = UiAgentPolicy.ConfirmationMode.SESSION_GRANT,
            screenWidth = 1080, screenHeight = 2400, density = 3f
        ))
    }

    @Test fun visualActionsRejectStaleOrSensitiveObservations() {
        val action = UiAgentPolicy.Action(
            kind = UiAgentPolicy.Kind.VISION_TAP,
            screenPackage = "com.example.editor",
            screenRevision = 7L,
            foregroundEpoch = 3L,
            x1 = 540, y1 = 900, x2 = 540, y2 = 900
        )
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(
            action.copy(screenRevision = -1L), emptyList(), "Trim clip",
            screenWidth = 1080, screenHeight = 2400, density = 3f
        ))
        assertEquals(UiAgentPolicy.Verdict.REJECT, UiAgentPolicy.validate(
            action, listOf("Enter OTP"), "Trim clip",
            screenWidth = 1080, screenHeight = 2400, density = 3f, sensitiveScreen = true
        ))
    }

    @Test fun unknownGestureKindsAreRejected() {
        assertFalse(UiAgentVisionPolicy.isSafeGesture("type_text", 540, 900, 540, 900, 1080, 2400, 3f, false))
    }
}
