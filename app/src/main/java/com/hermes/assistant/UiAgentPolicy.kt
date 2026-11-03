package com.hermes.assistant

import android.view.accessibility.AccessibilityEvent

/**
 * Strict validator for model-proposed UI actions. Screen text is data, never authority.
 * A model may only target controls actually present in the latest observed accessibility tree.
 */
internal object UiAgentPolicy {
    enum class Kind { OPEN_APP, TAP_TEXT, TAP_OCR, LONG_PRESS_OCR, SWIPE_OCR, DRAG_OCR, TAP_NODE, LONG_PRESS_NODE, SWIPE_NODE, DRAG_NODE, VISION_TAP, VISION_LONG_PRESS, VISION_SWIPE, VISION_DRAG, SCROLL, SCROLL_UNTIL_FOUND, TYPE_TEXT, BACK, HOME, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, WAIT, DONE }
    enum class Verdict { ALLOW, CONFIRM, REJECT }
    enum class ConfirmationMode { ALWAYS_ASK, RISKY_ASK, SESSION_GRANT, USER_REQUESTED_INSTALL }
    data class Action(
        val kind: Kind,
        val target: String = "",
        val direction: String = "",
        val text: String = "",
        val nodeIndex: Int = -1,
        val nodeFingerprint: String = "",
        val ocrIndex: Int = -1,
        val ocrFingerprint: String = "",
        val screenPackage: String = "",
        val fromAnchor: String = "",
        val toAnchor: String = "",
        val completionEvidence: String = "",
        val waitMs: Int = 1000,
        val maxScrolls: Int = 5,
        val screenRevision: Long = -1L,
        val foregroundEpoch: Long = -1L,
        val x1: Int = -1,
        val y1: Int = -1,
        val x2: Int = -1,
        val y2: Int = -1
    )
    data class VisibleNode(val index: Int, val clickable: Boolean, val longClickable: Boolean, val password: Boolean, val editable: Boolean, val fingerprint: String, val gestureBoundsSafe: Boolean = false, val label: String = "")
    data class VisibleOcr(val index: Int, val text: String, val fingerprint: String, val gestureBoundsSafe: Boolean = false)

    data class ScreenBounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

    fun isGestureWithinDisplay(
        x1: Float, y1: Float, x2: Float, y2: Float, screenWidth: Int, screenHeight: Int
    ): Boolean {
        if (screenWidth <= 0 || screenHeight <= 0 ||
            !x1.isFinite() || !y1.isFinite() || !x2.isFinite() || !y2.isFinite()) return false
        fun inside(x: Float, y: Float) =
            x >= 0f && y >= 0f && x < screenWidth.toFloat() && y < screenHeight.toFloat()
        return inside(x1, y1) && inside(x2, y2)
    }

    /** Maps OCR pixels from the captured screenshot into current display pixels.
     * Refuses invalid bounds and portrait/landscape mismatches rather than guessing.
     */
    fun mapOcrBoundsToScreen(
        left: Int, top: Int, right: Int, bottom: Int,
        imageWidth: Int, imageHeight: Int, screenWidth: Int, screenHeight: Int
    ): ScreenBounds? {
        if (imageWidth <= 0 || imageHeight <= 0 || screenWidth <= 0 || screenHeight <= 0 ||
            left < 0 || top < 0 || right <= left || bottom <= top ||
            right > imageWidth || bottom > imageHeight) return null
        val imageAspectCross = imageWidth.toLong() * screenHeight.toLong()
        val screenAspectCross = screenWidth.toLong() * imageHeight.toLong()
        val aspectDifference = kotlin.math.abs(imageAspectCross - screenAspectCross)
        val aspectScale = maxOf(imageAspectCross, screenAspectCross)
        if (aspectScale == 0L || aspectDifference * 100L > aspectScale * 10L) return null
        val mappedLeft = (left.toLong() * screenWidth / imageWidth).toInt()
        val mappedTop = (top.toLong() * screenHeight / imageHeight).toInt()
        val mappedRight = ((right.toLong() * screenWidth + imageWidth - 1L) / imageWidth).toInt()
        val mappedBottom = ((bottom.toLong() * screenHeight + imageHeight - 1L) / imageHeight).toInt()
        if (mappedLeft < 0 || mappedTop < 0 || mappedRight > screenWidth || mappedBottom > screenHeight ||
            mappedRight <= mappedLeft || mappedBottom <= mappedTop) return null
        return ScreenBounds(mappedLeft, mappedTop, mappedRight, mappedBottom)
    }

    private val blockedTargets = Regex(
        """\b(pay|payment|upi|wallet|bank|transfer|security|permissions?|biometric|password|passcode|otp|uninstall)\b""",
        RegexOption.IGNORE_CASE
    )

    private val consequential = Regex(
        """\b(pay|purchase|buy|delete|remove|send|share|post|publish|export|install|uninstall|subscribe|transfer|confirm|approve|allow|grant|order|checkout|payment|place order)\b""",
        RegexOption.IGNORE_CASE
    )

    private fun requestedInstallTokens(userGoal: String): Set<String> {
        val requested = Regex("""(?i)\binstall\s+(.+?)\s+from\s+(?:google\s+)?play\s*store\b""")
            .find(userGoal)?.groupValues?.getOrNull(1) ?: return emptySet()
        val stopWords = setOf("latest", "newest", "version", "app", "application", "the", "official")
        return Regex("""[\p{L}\p{N}]+""").findAll(requested.lowercase())
            .map { it.value }.filter { it !in stopWords }.toSet()
    }

    private fun isRequestedInstallNavigation(
        action: Action,
        targetLabel: String,
        userGoal: String,
        confirmationMode: ConfirmationMode
    ): Boolean {
        if (confirmationMode != ConfirmationMode.USER_REQUESTED_INSTALL ||
            action.screenPackage != "com.android.vending" || targetLabel.isBlank() ||
            consequential.containsMatchIn(targetLabel) || blockedTargets.containsMatchIn(targetLabel)) return false
        val expectedTokens = requestedInstallTokens(userGoal)
        if (expectedTokens.isEmpty()) return false
        val labelTokens = Regex("""[\p{L}\p{N}]+""").findAll(targetLabel.lowercase()).map { it.value }.toSet()
        return expectedTokens.all { it in labelTokens }
    }

    private fun isExplicitRequestedInstallTap(
        action: Action,
        visibleLabels: Collection<String>,
        userGoal: String,
        visibleNodes: Collection<VisibleNode>,
        visibleOcr: Collection<VisibleOcr>,
        confirmationMode: ConfirmationMode
    ): Boolean {
        if (confirmationMode != ConfirmationMode.USER_REQUESTED_INSTALL ||
            action.screenPackage != "com.android.vending" ||
            action.kind !in setOf(Kind.TAP_TEXT, Kind.TAP_OCR, Kind.TAP_NODE)) return false

        val targetLabel = when (action.kind) {
            Kind.TAP_TEXT -> action.target
            Kind.TAP_OCR -> visibleOcr.firstOrNull { it.index == action.ocrIndex }?.text.orEmpty()
            Kind.TAP_NODE -> visibleNodes.firstOrNull { it.index == action.nodeIndex }?.label.orEmpty()
            else -> ""
        }
        if (!targetLabel.trim().equals("Install", ignoreCase = true)) return false
        val installTargetCount = when (action.kind) {
            Kind.TAP_TEXT -> visibleLabels.count { it.trim().equals("Install", ignoreCase = true) }
            Kind.TAP_OCR -> visibleOcr.count { it.text.trim().equals("Install", ignoreCase = true) }
            Kind.TAP_NODE -> visibleNodes.count { it.label.trim().equals("Install", ignoreCase = true) }
            else -> 0
        }
        if (installTargetCount != 1) return false

        val requested = Regex("""(?i)\binstall\s+(.+?)\s+from\s+(?:google\s+)?play\s*store\b""")
            .find(userGoal)?.groupValues?.getOrNull(1) ?: return false
        val stopWords = setOf("latest", "newest", "version", "app", "application", "the", "official")
        val expectedTokens = Regex("""[\p{L}\p{N}]+""").findAll(requested.lowercase())
            .map { it.value }.filter { it !in stopWords }.toSet()
        if (expectedTokens.isEmpty()) return false

        val labels = (visibleLabels + visibleOcr.map { it.text } + visibleNodes.map { it.label })
            .map(String::trim).filter { it.isNotBlank() && !it.equals("Install", true) }
        val matchingTitles = labels.filter { label ->
            val tokens = Regex("""[\p{L}\p{N}]+""").findAll(label.lowercase()).map { it.value }.toSet()
            expectedTokens.all { it in tokens }
        }.distinctBy { label ->
            Regex("""[\p{L}\p{N}]+""").findAll(label.lowercase()).map { it.value }.sorted().joinToString(" ")
        }
        return matchingTitles.size == 1
    }

    fun validate(action: Action, visibleLabels: Collection<String>, userGoal: String, visibleNodes: Collection<VisibleNode> = emptyList(), allowedAppPackages: Set<String> = emptySet(), visibleOcr: Collection<VisibleOcr> = emptyList(), confirmationMode: ConfirmationMode = ConfirmationMode.RISKY_ASK, screenWidth: Int = 0, screenHeight: Int = 0, density: Float = 1f, sensitiveScreen: Boolean = false): Verdict {
        if (action.screenPackage == AppAccessPolicy.SYSTEM_UI_PACKAGE) {
            return validateSystemUiAction(action, visibleLabels, userGoal, visibleNodes, visibleOcr)
        }
        return when (action.kind) {
            Kind.OPEN_APP -> if (action.target.isNotBlank() && action.target in allowedAppPackages) Verdict.ALLOW else Verdict.REJECT
            Kind.TAP_OCR, Kind.LONG_PRESS_OCR -> {
                val target = visibleOcr.firstOrNull { it.index == action.ocrIndex }
                if (target == null || !target.gestureBoundsSafe || action.target.isBlank() || target.text != action.target ||
                    target.fingerprint != action.ocrFingerprint || blockedTargets.containsMatchIn(action.target)) Verdict.REJECT
                else if (action.kind == Kind.TAP_OCR && isExplicitRequestedInstallTap(action, visibleLabels, userGoal, visibleNodes, visibleOcr, confirmationMode)) Verdict.ALLOW
                else if (action.kind == Kind.TAP_OCR && isRequestedInstallNavigation(action, action.target, userGoal, confirmationMode)) Verdict.ALLOW
                else if (sessionGrantAllows(confirmationMode, userGoal, action.target)) Verdict.ALLOW
                else Verdict.CONFIRM
            }
            Kind.SWIPE_OCR -> {
                val target = visibleOcr.firstOrNull { it.index == action.ocrIndex }
                if (target == null || !target.gestureBoundsSafe || action.target.isBlank() || target.text != action.target ||
                    target.fingerprint != action.ocrFingerprint || blockedTargets.containsMatchIn(action.target) ||
                    action.direction.lowercase() !in setOf("left", "right", "up", "down")) Verdict.REJECT
                else Verdict.CONFIRM
            }
            Kind.DRAG_OCR -> {
                val target = visibleOcr.firstOrNull { it.index == action.ocrIndex }
                val anchors = setOf("left", "right", "top", "bottom", "center")
                if (target == null || !target.gestureBoundsSafe || action.target.isBlank() || target.text != action.target ||
                    target.fingerprint != action.ocrFingerprint || blockedTargets.containsMatchIn(action.target) ||
                    action.fromAnchor.lowercase() !in anchors || action.toAnchor.lowercase() !in anchors ||
                    action.fromAnchor.equals(action.toAnchor, ignoreCase = true)) Verdict.REJECT
                else Verdict.CONFIRM
            }
            Kind.TAP_TEXT -> {
                if (action.target.isBlank() || action.target.length > 100 || blockedTargets.containsMatchIn(action.target) ||
                    visibleLabels.none { it.equals(action.target, ignoreCase = true) }) Verdict.REJECT
                else if (confirmationMode == ConfirmationMode.USER_REQUESTED_INSTALL &&
                    action.target.trim().equals("Install", ignoreCase = true)) {
                    if (action.screenPackage != "com.android.vending") Verdict.REJECT
                    else if (isExplicitRequestedInstallTap(action, visibleLabels, userGoal, visibleNodes, visibleOcr, confirmationMode)) Verdict.ALLOW
                    else Verdict.CONFIRM
                }
                else if (confirmationMode == ConfirmationMode.USER_REQUESTED_INSTALL &&
                    action.target.trim().equals("Buy", ignoreCase = true)) Verdict.REJECT
                else if (isExplicitRequestedInstallTap(action, visibleLabels, userGoal, visibleNodes, visibleOcr, confirmationMode)) Verdict.ALLOW
                else if (confirmationMode == ConfirmationMode.ALWAYS_ASK || consequential.containsMatchIn(action.target)) Verdict.CONFIRM
                else Verdict.ALLOW
            }
            Kind.TAP_NODE -> {
                val node = visibleNodes.firstOrNull { it.index == action.nodeIndex }
                if (node == null || !node.gestureBoundsSafe || node.password || node.editable || blockedTargets.containsMatchIn(node.label) || node.fingerprint.isBlank() || node.fingerprint != action.nodeFingerprint) Verdict.REJECT
                else if (isExplicitRequestedInstallTap(action, visibleLabels, userGoal, visibleNodes, visibleOcr, confirmationMode)) Verdict.ALLOW
                else if (isRequestedInstallNavigation(action, node.label, userGoal, confirmationMode)) Verdict.ALLOW
                else if (sessionGrantAllows(confirmationMode, userGoal, node.label)) Verdict.ALLOW
                else Verdict.CONFIRM
            }
            Kind.LONG_PRESS_NODE -> {
                val node = visibleNodes.firstOrNull { it.index == action.nodeIndex }
                if (node == null || !node.gestureBoundsSafe || node.password || node.editable || blockedTargets.containsMatchIn(node.label) || node.fingerprint.isBlank() || node.fingerprint != action.nodeFingerprint) Verdict.REJECT
                else if (sessionGrantAllows(confirmationMode, userGoal, node.label)) Verdict.ALLOW
                else Verdict.CONFIRM
            }
            Kind.SWIPE_NODE -> {
                val node = visibleNodes.firstOrNull { it.index == action.nodeIndex }
                if (node == null || !node.gestureBoundsSafe || node.password || node.editable || blockedTargets.containsMatchIn(node.label) || node.fingerprint.isBlank() || node.fingerprint != action.nodeFingerprint || action.direction.lowercase() !in setOf("left", "right", "up", "down")) Verdict.REJECT
                else if (sessionGrantAllows(confirmationMode, userGoal, node.label)) Verdict.ALLOW
                else Verdict.CONFIRM
            }
            Kind.DRAG_NODE -> {
                val node = visibleNodes.firstOrNull { it.index == action.nodeIndex }
                val anchors = setOf("left", "right", "top", "bottom", "center")
                if (node == null || !node.gestureBoundsSafe || node.password || node.editable || blockedTargets.containsMatchIn(node.label) ||
                    node.fingerprint.isBlank() || node.fingerprint != action.nodeFingerprint ||
                    action.fromAnchor.lowercase() !in anchors || action.toAnchor.lowercase() !in anchors ||
                    action.fromAnchor.equals(action.toAnchor, ignoreCase = true)) Verdict.REJECT
                else if (sessionGrantAllows(confirmationMode, userGoal, node.label)) Verdict.ALLOW
                else Verdict.CONFIRM
            }
            Kind.VISION_TAP, Kind.VISION_LONG_PRESS, Kind.VISION_SWIPE, Kind.VISION_DRAG -> {
                val gesture = when (action.kind) {
                    Kind.VISION_TAP -> "tap"
                    Kind.VISION_LONG_PRESS -> "long_press"
                    Kind.VISION_SWIPE -> "swipe"
                    else -> "drag"
                }
                if (action.screenPackage.isBlank() || action.screenRevision < 0L || action.foregroundEpoch < 0L ||
                    !UiAgentVisionPolicy.isSafeGesture(
                        gesture, action.x1, action.y1, action.x2, action.y2,
                        screenWidth, screenHeight, density, sensitiveScreen
                    )) Verdict.REJECT else Verdict.CONFIRM
            }
            Kind.SCROLL -> if (action.direction.lowercase() in setOf("up", "down")) Verdict.ALLOW else Verdict.REJECT
            Kind.SCROLL_UNTIL_FOUND -> {
                val target = action.target.trim()
                if (target.isBlank() || target.length > 100 || blockedTargets.containsMatchIn(target) ||
                    action.direction.lowercase() !in setOf("up", "down") || action.maxScrolls !in 1..8 ||
                    visibleLabels.any { it.trim().equals(target, ignoreCase = true) }) Verdict.REJECT
                else Verdict.ALLOW
            }
            Kind.TYPE_TEXT -> {
                val text = action.text.trim()
                val node = visibleNodes.firstOrNull { it.index == action.nodeIndex }
                if (node == null || !node.editable || node.password || node.fingerprint.isBlank() ||
                    node.fingerprint != action.nodeFingerprint || blockedTargets.containsMatchIn(node.label) || visibleLabels.any { blockedTargets.containsMatchIn(it) } ||
                    text.isBlank() || text.length > 500 || !userGoal.contains(text, ignoreCase = true) ||
                    blockedTargets.containsMatchIn(text) ||
                    Regex("(?i)(password|passcode|otp|cvv|api[_ -]?key|access[_ -]?token)").containsMatchIn(text) ||
                    Regex("""\b(?:\d[ -]*?){13,19}\b""").containsMatchIn(text) ||
                    Regex("""\b\d{4,12}\b""").containsMatchIn(text)) Verdict.REJECT
                else Verdict.ALLOW
            }
            Kind.DONE -> {
                val evidence = action.completionEvidence.trim()
                if (evidence.isBlank() || evidence.length > 120 ||
                    visibleLabels.none { it.trim().equals(evidence, ignoreCase = true) }) Verdict.REJECT
                else Verdict.ALLOW
            }
            Kind.BACK, Kind.HOME, Kind.RECENTS, Kind.NOTIFICATIONS, Kind.QUICK_SETTINGS -> Verdict.ALLOW
            Kind.WAIT -> if (action.waitMs in 250..8000) Verdict.ALLOW else Verdict.REJECT
        }
    }


    // poka-yoke: only a narrow exact label allowlist can be interacted with in System UI [control]
    private val systemUiLabelPattern = Regex(
        """(?i)^(wi-?fi|internet|bluetooth|airplane mode|do not disturb|battery saver|flashlight|location|auto-rotate|screen record|hotspot|mobile data|brightness)(?:\s*(?:,|:|-)?\s*(?:is\s+)?(?:on|off|enabled|disabled|connected|disconnected|available|unavailable))?$"""
    )

    fun isSafeSystemUiLabel(label: String): Boolean =
        label.trim().replace(Regex("""\s+"""), " ").let { systemUiLabelPattern.matches(it) }

    fun isSystemUiTargetRequested(label: String, userGoal: String): Boolean {
        val normalized = label.trim().replace(Regex("""\s+"""), " ")
        val base = listOf(
            "Wi-Fi", "Internet", "Bluetooth", "Airplane mode", "Do Not Disturb",
            "Battery Saver", "Flashlight", "Location", "Auto-rotate", "Screen record",
            "Hotspot", "Mobile data", "Brightness"
        ).firstOrNull { normalized.equals(it, true) || normalized.startsWith(it + ",", true) ||
            normalized.startsWith(it + " ", true) } ?: return false
        val intent = when (base.lowercase()) {
            "wi-fi", "internet" -> Regex("""(?i)\b(wi[ -]?fi|internet|wireless)\b""")
            "bluetooth" -> Regex("""(?i)\bbluetooth\b""")
            "airplane mode" -> Regex("""(?i)\b(airplane|flight)\s*mode\b""")
            "do not disturb" -> Regex("""(?i)\b(do not disturb|dnd)\b""")
            "battery saver" -> Regex("""(?i)\b(battery saver|power saving)\b""")
            "flashlight" -> Regex("""(?i)\b(flashlight|torch)\b""")
            "location" -> Regex("""(?i)\blocation\b""")
            "auto-rotate" -> Regex("""(?i)\b(auto.?rotate|rotation|rotate)\b""")
            "screen record" -> Regex("""(?i)\b(screen record|screen recording)\b""")
            "hotspot" -> Regex("""(?i)\bhotspot\b""")
            "mobile data" -> Regex("""(?i)\b(mobile data|cellular data)\b""")
            "brightness" -> Regex("""(?i)\b(brightness|brighter|dimmer|dim screen)\b""")
            else -> return false
        }
        return intent.containsMatchIn(userGoal)
    }

    /** System UI can show private notifications; only explicitly requested quick-setting controls pass. */
    private fun systemUiToggleDirection(userGoal: String): Int {
        val wantsOn = Regex("""(?i)\b(turn on|enable|activate|connect|on)\b""").containsMatchIn(userGoal)
        val wantsOff = Regex("""(?i)\b(turn off|disable|deactivate|disconnect|off)\b""").containsMatchIn(userGoal)
        return when {
            wantsOn && !wantsOff -> 1
            wantsOff && !wantsOn -> -1
            else -> 0
        }
    }

    private fun brightnessDirection(userGoal: String): Int {
        val increase = Regex("""(?i)\b(brighter|increase|raise|higher|up)\b""").containsMatchIn(userGoal)
        val decrease = Regex("""(?i)\b(dimmer|decrease|lower|reduce|dim)\b""").containsMatchIn(userGoal)
        return when {
            increase && !decrease -> 1
            decrease && !increase -> -1
            else -> 0
        }
    }

    fun isSystemUiTargetSatisfied(label: String, userGoal: String): Boolean {
        if (!isSafeSystemUiLabel(label) || !isSystemUiTargetRequested(label, userGoal)) return false
        val direction = systemUiToggleDirection(userGoal)
        if (direction == 0) return false
        val state = Regex("""(?i)\b(on|off|enabled|disabled|connected|disconnected)\b""")
            .find(label)?.value?.lowercase() ?: return false
        return if (direction > 0) state in setOf("on", "enabled", "connected")
            else state in setOf("off", "disabled", "disconnected")
    }

    private fun systemUiCategory(label: String): String? {
        val normalized = label.trim().lowercase()
        return when {
            normalized.startsWith("wi-fi") || normalized.startsWith("wifi") || normalized.startsWith("internet") -> "network"
            normalized.startsWith("bluetooth") -> "bluetooth"
            normalized.startsWith("airplane mode") -> "airplane"
            normalized.startsWith("do not disturb") -> "dnd"
            normalized.startsWith("battery saver") -> "battery"
            normalized.startsWith("flashlight") -> "flashlight"
            normalized.startsWith("location") -> "location"
            normalized.startsWith("auto-rotate") -> "rotation"
            normalized.startsWith("screen record") -> "recording"
            normalized.startsWith("hotspot") -> "hotspot"
            normalized.startsWith("mobile data") -> "mobile-data"
            normalized.startsWith("brightness") -> "brightness"
            else -> null
        }
    }

    fun isQuickSettingsOpenGoal(userGoal: String): Boolean =
        Regex("""(?i)\b(open|show|expand|pull down)\b""").containsMatchIn(userGoal) &&
            Regex("""(?i)\bquick settings\b""").containsMatchIn(userGoal)

    fun isSystemUiGoalVerified(visibleLabels: Collection<String>, userGoal: String): Boolean {
        if (isQuickSettingsOpenGoal(userGoal)) return visibleLabels.any { isSafeSystemUiLabel(it) }
        val knownTargets = listOf(
            "Wi-Fi", "Internet", "Bluetooth", "Airplane mode", "Do Not Disturb",
            "Battery Saver", "Flashlight", "Location", "Auto-rotate", "Screen record",
            "Hotspot", "Mobile data", "Brightness"
        )
        val requestedCategories = knownTargets.filter { isSystemUiTargetRequested(it, userGoal) }
            .mapNotNull(::systemUiCategory).toSet()
        if (requestedCategories.isEmpty()) return false
        val wantsOn = Regex("""(?i)\b(turn on|enable|activate|connect|on)\b""").containsMatchIn(userGoal)
        val wantsOff = Regex("""(?i)\b(turn off|disable|deactivate|disconnect|off)\b""").containsMatchIn(userGoal)
        if (wantsOn == wantsOff) return false
        val requiredStates = if (wantsOn) setOf("on", "enabled", "connected") else setOf("off", "disabled", "disconnected")
        val statePattern = Regex("""(?i)\b(on|off|enabled|disabled|connected|disconnected)\b""")
        val observedCategories = visibleLabels.filter { isSafeSystemUiLabel(it) }.filter { label ->
            val state = statePattern.find(label)?.value?.lowercase()
            state != null && state in requiredStates
        }.mapNotNull(::systemUiCategory).toSet()
        return requestedCategories.all { it in observedCategories }
    }

    fun isSystemUiCompletionVerified(evidence: String, userGoal: String): Boolean =
        isSafeSystemUiLabel(evidence) && isSystemUiTargetRequested(evidence, userGoal) &&
            isSystemUiGoalVerified(listOf(evidence), userGoal)

    private fun validateSystemUiAction(
        action: Action, visibleLabels: Collection<String>, userGoal: String,
        visibleNodes: Collection<VisibleNode>, visibleOcr: Collection<VisibleOcr>
    ): Verdict {
        fun allowed(label: String) =
            isSafeSystemUiLabel(label) && isSystemUiTargetRequested(label, userGoal)
        return when (action.kind) {
            Kind.TAP_NODE -> {
                val node = visibleNodes.firstOrNull { it.index == action.nodeIndex }
                if (node != null && node.clickable && !node.password && !node.editable &&
                    node.gestureBoundsSafe && node.fingerprint.isNotBlank() &&
                    node.fingerprint == action.nodeFingerprint && allowed(node.label) &&
                    !node.label.equals("Brightness", true) &&
                    systemUiToggleDirection(userGoal) != 0 &&
                    !isSystemUiTargetSatisfied(node.label, userGoal)) Verdict.CONFIRM
                else Verdict.REJECT
            }
            Kind.DRAG_NODE -> {
                val node = visibleNodes.firstOrNull { it.index == action.nodeIndex }
                val anchors = setOf("left", "center", "right")
                val positions = mapOf("left" to 0, "center" to 1, "right" to 2)
                val direction = brightnessDirection(userGoal)
                val delta = (positions[action.toAnchor.lowercase()] ?: 0) -
                    (positions[action.fromAnchor.lowercase()] ?: 0)
                if (node != null && !node.password && !node.editable && node.gestureBoundsSafe &&
                    node.fingerprint.isNotBlank() && node.fingerprint == action.nodeFingerprint &&
                    node.label.equals("Brightness", true) && allowed(node.label) &&
                    action.fromAnchor.lowercase() in anchors && action.toAnchor.lowercase() in anchors &&
                    delta != 0 && direction != 0 && delta * direction > 0) Verdict.CONFIRM
                else Verdict.REJECT
            }
            Kind.BACK -> Verdict.ALLOW
            Kind.WAIT -> if (action.waitMs in 250..8000) Verdict.ALLOW else Verdict.REJECT
            Kind.DONE -> if (action.completionEvidence.isNotBlank() &&
                visibleLabels.any { it.trim().equals(action.completionEvidence.trim(), true) } &&
                isSystemUiGoalVerified(visibleLabels, userGoal)) Verdict.ALLOW else Verdict.REJECT
            else -> Verdict.REJECT
        }
    }

    private fun sessionGrantAllows(mode: ConfirmationMode, userGoal: String, targetLabel: String): Boolean =
        mode == ConfirmationMode.SESSION_GRANT &&
            !consequential.containsMatchIn(userGoal) && !consequential.containsMatchIn(targetLabel)

    fun isFreshScreen(observedRevision: Long, currentRevision: Long, observedPackage: String, currentPackage: String): Boolean =
        observedRevision >= 0L && observedRevision == currentRevision &&
            observedPackage.isNotBlank() && observedPackage == currentPackage

    fun isPlannerResponseFresh(observedRevision: Long, currentRevision: Long, observedPackage: String, currentPackage: String): Boolean =
        isFreshScreen(observedRevision, currentRevision, observedPackage, currentPackage)

    fun isTreeObservationFresh(captureRevision: Long, currentRevision: Long, capturePackage: String, currentPackage: String): Boolean =
        isFreshScreen(captureRevision, currentRevision, capturePackage, currentPackage)

    fun isActionObservationFresh(observedRevision: Long, currentRevision: Long, observedPackage: String, currentPackage: String): Boolean =
        isFreshScreen(observedRevision, currentRevision, observedPackage, currentPackage)

    fun isRelevantAccessibilityEvent(eventType: Int): Boolean = eventType in setOf(
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
        AccessibilityEvent.TYPE_WINDOWS_CHANGED,
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
        AccessibilityEvent.TYPE_VIEW_SCROLLED,
        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
        AccessibilityEvent.TYPE_VIEW_CLICKED,
        AccessibilityEvent.TYPE_VIEW_SELECTED,
        AccessibilityEvent.TYPE_VIEW_FOCUSED
    )

    fun shouldTrackAccessibilityEvent(eventPackage: String, ownPackage: String, eventWindowId: Int, activeWindowId: Int): Boolean =
        eventPackage.isNotBlank() &&
            (eventPackage != ownPackage || (activeWindowId >= 0 && eventWindowId == activeWindowId))

    fun mayExecute(generationMatches: Boolean, stopControlOn: Boolean, nowMs: Long, grantExpiresAt: Long?): Boolean =
        generationMatches && !stopControlOn && (grantExpiresAt == null || nowMs < grantExpiresAt)


    fun mayResumeAfterAction(
        generationMatches: Boolean, stopControlOn: Boolean,
        expectedPackage: String, currentPackage: String,
        expectedForegroundEpoch: Long, currentForegroundEpoch: Long
    ): Boolean =
        generationMatches && !stopControlOn &&
            expectedPackage.isNotBlank() && expectedPackage == currentPackage &&
            expectedForegroundEpoch == currentForegroundEpoch

    fun isOpenAppOutcomeVerified(requestedPackage: String, foregroundPackage: String): Boolean =
        requestedPackage.isNotBlank() && requestedPackage == foregroundPackage

    fun invalidateGeneration(currentGeneration: Int): Int =
        if (currentGeneration == Int.MAX_VALUE) Int.MIN_VALUE else currentGeneration + 1

    fun mayResumeAfterWait(
        generationMatches: Boolean, stopControlOn: Boolean, nowMs: Long, grantExpiresAt: Long?,
        expectedPackage: String, currentPackage: String,
        expectedForegroundEpoch: Long, currentForegroundEpoch: Long
    ): Boolean =
        mayExecute(generationMatches, stopControlOn, nowMs, grantExpiresAt) &&
            mayResumeAfterAction(true, false, expectedPackage, currentPackage,
                expectedForegroundEpoch, currentForegroundEpoch)

    fun actionAccepted(actionSucceeded: Boolean): Boolean = actionSucceeded

    fun mayContinue(completedSteps: Int, maxSteps: Int = 40): Boolean =
        completedSteps >= 0 && completedSteps < maxSteps
    fun progressMessage(completedSteps: Int): String =
        "Action accepted • rechecking screen • step " + completedSteps.coerceIn(0, 40) + "/40"

    /** Audit descriptions intentionally omit user-entered text and visible labels. */
    fun auditLabel(action: Action): String {
        val detail = when (action.kind) {
            Kind.TAP_NODE, Kind.LONG_PRESS_NODE, Kind.SWIPE_NODE, Kind.DRAG_NODE -> " node #" + action.nodeIndex
            Kind.TAP_OCR, Kind.LONG_PRESS_OCR, Kind.SWIPE_OCR, Kind.DRAG_OCR -> " OCR #" + action.ocrIndex
            Kind.VISION_TAP, Kind.VISION_LONG_PRESS, Kind.VISION_SWIPE, Kind.VISION_DRAG -> " visual target"
            Kind.TYPE_TEXT -> " field #" + action.nodeIndex
            else -> ""
        }
        return "UI Agent " + action.kind.name + detail
    }

}
