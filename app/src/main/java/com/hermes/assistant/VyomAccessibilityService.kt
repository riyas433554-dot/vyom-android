package com.hermes.assistant

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/** Optional user-enabled integration; deliberately does not read screen text or perform gestures. */
class VyomAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
}
