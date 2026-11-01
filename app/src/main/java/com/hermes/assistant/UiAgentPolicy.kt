package com.hermes.assistant

/**
 * Strict validator for model-proposed UI actions. Screen text is data, never authority.
 * A model may only target controls actually present in the latest observed accessibility tree.
 */
internal object UiAgentPolicy {
    enum class Kind { OPEN_APP, TAP_TEXT, TAP_OCR, LONG_PRESS_OCR, TAP_NODE, LONG_PRESS_NODE, SWIPE_NODE, SCROLL, TYPE_TEXT, BACK, WAIT, DONE }
    enum class Verdict { ALLOW, CONFIRM, REJECT }
    data class Action(
        val kind: Kind,
        val target: String = "",
        val direction: String = "",
        val text: String = "",
        val nodeIndex: Int = -1,
        val nodeFingerprint: String = "",
        val ocrIndex: Int = -1,
        val ocrFingerprint: String = "",
        val screenPackage: String = ""
    )
    data class VisibleNode(val index: Int, val clickable: Boolean, val longClickable: Boolean, val password: Boolean, val editable: Boolean, val fingerprint: String, val gestureBoundsSafe: Boolean = false, val label: String = "")
    data class VisibleOcr(val index: Int, val text: String, val fingerprint: String)

    private val blockedTargets = Regex(
        """\b(pay|payment|upi|wallet|bank|transfer|security|permissions?|biometric|password|passcode|otp|uninstall)\b""",
        RegexOption.IGNORE_CASE
    )

    private val consequential = Regex(
        """\b(pay|purchase|buy|delete|remove|send|share|post|publish|export|install|uninstall|subscribe|transfer|confirm|approve|allow|grant|order|checkout|payment|place order)\b""",
        RegexOption.IGNORE_CASE
    )

    fun validate(action: Action, visibleLabels: Collection<String>, userGoal: String, visibleNodes: Collection<VisibleNode> = emptyList(), allowedAppPackages: Set<String> = emptySet(), visibleOcr: Collection<VisibleOcr> = emptyList()): Verdict {
        return when (action.kind) {
            Kind.OPEN_APP -> if (action.target.isNotBlank() && action.target in allowedAppPackages) Verdict.ALLOW else Verdict.REJECT
            Kind.TAP_OCR, Kind.LONG_PRESS_OCR -> {
                val target = visibleOcr.firstOrNull { it.index == action.ocrIndex }
                if (target == null || action.target.isBlank() || target.text != action.target || target.fingerprint != action.ocrFingerprint || blockedTargets.containsMatchIn(action.target)) Verdict.REJECT
                else Verdict.CONFIRM
            }
            Kind.TAP_TEXT -> {
                if (action.target.isBlank() || action.target.length > 100 || blockedTargets.containsMatchIn(action.target) ||
                    visibleLabels.none { it.equals(action.target, ignoreCase = true) }) Verdict.REJECT
                else if (consequential.containsMatchIn(action.target)) Verdict.CONFIRM
                else Verdict.ALLOW
            }
            Kind.TAP_NODE -> {
                val node = visibleNodes.firstOrNull { it.index == action.nodeIndex }
                if (node == null || !node.gestureBoundsSafe || node.password || node.editable || blockedTargets.containsMatchIn(node.label) || node.fingerprint.isBlank() || node.fingerprint != action.nodeFingerprint) Verdict.REJECT
                else Verdict.CONFIRM
            }
            Kind.LONG_PRESS_NODE -> {
                val node = visibleNodes.firstOrNull { it.index == action.nodeIndex }
                if (node == null || !node.gestureBoundsSafe || node.password || node.editable || blockedTargets.containsMatchIn(node.label) || node.fingerprint.isBlank() || node.fingerprint != action.nodeFingerprint) Verdict.REJECT
                else Verdict.CONFIRM
            }
            Kind.SWIPE_NODE -> {
                val node = visibleNodes.firstOrNull { it.index == action.nodeIndex }
                if (node == null || !node.gestureBoundsSafe || node.password || node.editable || blockedTargets.containsMatchIn(node.label) || node.fingerprint.isBlank() || node.fingerprint != action.nodeFingerprint || action.direction.lowercase() !in setOf("left", "right", "up", "down")) Verdict.REJECT
                else Verdict.CONFIRM
            }
            Kind.SCROLL -> if (action.direction.lowercase() in setOf("up", "down")) Verdict.ALLOW else Verdict.REJECT
            Kind.TYPE_TEXT -> {
                val text = action.text.trim()
                val node = visibleNodes.firstOrNull { it.index == action.nodeIndex }
                if (node == null || !node.editable || node.password || node.fingerprint.isBlank() ||
                    node.fingerprint != action.nodeFingerprint || blockedTargets.containsMatchIn(node.label) ||
                    text.isBlank() || text.length > 500 || !userGoal.contains(text, ignoreCase = true) ||
                    blockedTargets.containsMatchIn(text) ||
                    Regex("(?i)(password|passcode|otp|cvv|api[_ -]?key|access[_ -]?token)").containsMatchIn(text) ||
                    Regex("""\b(?:\d[ -]*?){13,19}\b""").containsMatchIn(text) ||
                    Regex("""\b\d{4,12}\b""").containsMatchIn(text)) Verdict.REJECT
                else Verdict.ALLOW
            }
            Kind.BACK, Kind.WAIT, Kind.DONE -> Verdict.ALLOW
        }
    }

    fun mayContinue(completedSteps: Int, maxSteps: Int = 12): Boolean =
        completedSteps >= 0 && completedSteps < maxSteps
}
