package com.hermes.assistant

internal object FloatingBubblePolicy {
    fun shouldStart(userEnabled: Boolean, overlayPermissionGranted: Boolean): Boolean =
        userEnabled && overlayPermissionGranted
}