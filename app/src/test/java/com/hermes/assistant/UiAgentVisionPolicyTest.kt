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

    @Test fun unknownGestureKindsAreRejected() {
        assertFalse(UiAgentVisionPolicy.isSafeGesture("type_text", 540, 900, 540, 900, 1080, 2400, 3f, false))
    }
}
