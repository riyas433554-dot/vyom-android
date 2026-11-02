package com.hermes.assistant

/**
 * Strict validator for model-proposed UI actions. Screen text is data, never authority.
 * A model may only target controls actually present in the latest observed accessibility tree.
 */
internal object UiAgentPolicy {
    enum class Kind { OPEN_APP, TAP_TEXT, TAP_OCR, LONG_PRESS_OCR, SWIPE_OCR, DRAG_OCR, TAP_NODE, LONG_PRESS_NODE, SWIPE_NODE, DRAG_NODE, SCROLL, TYPE_TEXT, BACK, WAIT, DONE }
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
        val waitMs: Int = 1000
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

        val requested = Regex(
            """(?i)\\binstall\\s+(.+?)\\s+from\\s+(?:google\\s+)?play\\s*store\\b"""
        ).find(userGoal)?.groupValues?.getOrNull(1) ?: return false
        val stopWords = setOf("latest", "newest", "version", "app", "application", "the", "official")
        val expectedTokens = Regex("[\\\\p{L}\\\\p{N}]+").findAll(requested.lowercase())
            .map { it.value }.filter { it !in stopWords }.toSet()
        if (expectedTokens.isEmpty()) return false

        val labels = (visibleLabels + visibleOcr.map { it.text } + visibleNodes.map { it.label })
            .map(String::trim).filter { it.isNotBlank() && !it.equals("Install", true) }
        val matchingTitles = labels.filter { label ->
            val tokens = Regex("[\\\\p{L}\\\\p{N}]+").findAll(label.lowercase()).map { it.value }.toSet()
            expectedTokens.all { it in tokens }
        }.distinctBy { label ->
            Regex("[\\\\p{L}\\\\p{N}]+").findAll(label.lowercase()).map { it.value }.sorted().joinToString(" ")
        }
        return matchingTitles.size == 1
    }

    fun validate(action: Action, visibleLabels: Collection<String>, userGoal: String, visibleNodes: Collection<VisibleNode> = emptyList(), allowedAppPackages: Set<String> = emptySet(), visibleOcr: Collection<VisibleOcr> = emptyList(), confirmationMode: ConfirmationMode = ConfirmationMode.RISKY_ASK): Verdict {
        return when (action.kind) {
            Kind.OPEN_APP -> if (action.target.isNotBlank() && action.target in allowedAppPackages) Verdict.ALLOW else Verdict.REJECT
            Kind.TAP_OCR, Kind.LONG_PRESS_OCR -> {
                val target = visibleOcr.firstOrNull { it.index == action.ocrIndex }
                if (target == null || !target.gestureBoundsSafe || action.target.isBlank() || target.text != action.target ||
                    target.fingerprint != action.ocrFingerprint || blockedTargets.containsMatchIn(action.target)) Verdict.REJECT
                else if (action.kind == Kind.TAP_OCR && isExplicitRequestedInstallTap(action, visibleLabels, userGoal, visibleNodes, visibleOcr, confirmationMode)) Verdict.ALLOW
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
                else if (isExplicitRequestedInstallTap(action, visibleLabels, userGoal, visibleNodes, visibleOcr, confirmationMode)) Verdict.ALLOW
                else if (confirmationMode == ConfirmationMode.ALWAYS_ASK || consequential.containsMatchIn(action.target)) Verdict.CONFIRM
                else Verdict.ALLOW
            }
            Kind.TAP_NODE -> {
                val node = visibleNodes.firstOrNull { it.index == action.nodeIndex }
                if (node == null || !node.gestureBoundsSafe || node.password || node.editable || blockedTargets.containsMatchIn(node.label) || node.fingerprint.isBlank() || node.fingerprint != action.nodeFingerprint) Verdict.REJECT
                else if (isExplicitRequestedInstallTap(action, visibleLabels, userGoal, visibleNodes, visibleOcr, confirmationMode)) Verdict.ALLOW
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
            Kind.SCROLL -> if (action.direction.lowercase() in setOf("up", "down")) Verdict.ALLOW else Verdict.REJECT
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
            Kind.BACK -> Verdict.ALLOW
            Kind.WAIT -> if (action.waitMs in 250..8000) Verdict.ALLOW else Verdict.REJECT
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

    fun shouldTrackAccessibilityEvent(eventPackage: String, ownPackage: String, eventWindowId: Int, activeWindowId: Int): Boolean =
        eventPackage.isNotBlank() &&
            (eventPackage != ownPackage || (activeWindowId >= 0 && eventWindowId == activeWindowId))

    fun mayExecute(generationMatches: Boolean, stopControlOn: Boolean, nowMs: Long, grantExpiresAt: Long?): Boolean =
        generationMatches && !stopControlOn && (grantExpiresAt == null || nowMs < grantExpiresAt)


    fun mayResumeAfterWait(
        generationMatches: Boolean, stopControlOn: Boolean, nowMs: Long, grantExpiresAt: Long?,
        expectedPackage: String, currentPackage: String,
        expectedForegroundEpoch: Long, currentForegroundEpoch: Long
    ): Boolean =
        mayExecute(generationMatches, stopControlOn, nowMs, grantExpiresAt) &&
            expectedPackage.isNotBlank() && expectedPackage == currentPackage &&
            expectedForegroundEpoch == currentForegroundEpoch

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
            Kind.TYPE_TEXT -> " field #" + action.nodeIndex
            else -> ""
        }
        return "UI Agent " + action.kind.name + detail
    }

}
