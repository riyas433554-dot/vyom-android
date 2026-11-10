package com.hermes.assistant

/**
 * Pure four-lane intent router.
 *
 * Routing is not execution authorization: all consequential side effects stay
 * behind the existing one-time approval token and review policy.
 */
internal object VyomFactoryLaneRouter {
    enum class Origin { DIRECT_OWNER_COMMAND, COMPLAINT }

    enum class Lane(val priorityRank: Int) {
        INSTANT(0),
        SCHEDULED(1),
        PERMANENT(2),
        CUSTOM(3)
    }

    data class Request(
        val origin: Origin,
        val text: String,
        val scheduleApproved: Boolean = false,
        val permanentDutyApproved: Boolean = false,
        val customScopeApproved: Boolean = false
    )

    data class Decision(
        val lane: Lane,
        val reason: String,
        val mayStartPlanning: Boolean,
        /** Always true; lane classification never grants side-effect authority. */
        val sideEffectsRequireExistingApproval: Boolean = true
    )

    private val instantSignal = Regex(
        "\\b(?:now|right\\s+now|immediately|urgent(?:ly)?|asap|at\\s+once|abhi|turant)\\b",
        RegexOption.IGNORE_CASE
    )
    private val scheduleSignal = Regex(
        "\\b(?:schedule(?:d)?|daily|weekly|monthly|tomorrow|every\\s+(?:day|week|month|monday|tuesday|wednesday|thursday|friday|saturday|sunday|\\d+\\s+(?:minutes?|hours?|days?|weeks?))|at\\s+\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)\\b",
        RegexOption.IGNORE_CASE
    )
    private val permanentSignal = Regex(
        "\\b(?:always[ -]on|permanent(?:ly)?|standing\\s+duty|continuous(?:ly)?|keep\\s+monitoring|ongoing\\s+monitor)\\b",
        RegexOption.IGNORE_CASE
    )
    private val negatedUrgency = Regex(
        "\\b(?:not\\s+(?:urgent|immediately|now|asap)|no\\s+urgency)\\b",
        RegexOption.IGNORE_CASE
    )
    private val htmlComment = Regex("<!--.*?-->", setOf(RegexOption.DOT_MATCHES_ALL))

    fun route(request: Request): Decision {
        require(request.text.isNotBlank()) { "Lane routing text is required" }
        require(request.text.length <= MAX_ROUTING_TEXT) { "Lane routing text exceeds the supported size" }

        val clean = request.text.replace(htmlComment, " ")
        val urgencyText = clean.replace(negatedUrgency, " ")
        val isInstant = instantSignal.containsMatchIn(urgencyText)
        val isScheduled = scheduleSignal.containsMatchIn(clean)
        val isPermanent = permanentSignal.containsMatchIn(clean)
        val signalCount = listOf(isInstant, isScheduled, isPermanent).count { it }

        if (signalCount > 1) {
            return Decision(
                lane = Lane.CUSTOM,
                reason = "conflicting_lane_intent",
                mayStartPlanning = false
            )
        }

        return when {
            isPermanent -> Decision(
                lane = Lane.PERMANENT,
                reason = if (request.permanentDutyApproved) "owner_approved_permanent_duty"
                    else "permanent_duty_requires_owner_approval",
                mayStartPlanning = request.permanentDutyApproved
            )
            isScheduled -> Decision(
                lane = Lane.SCHEDULED,
                reason = if (request.scheduleApproved) "owner_approved_schedule"
                    else "schedule_requires_owner_approval",
                mayStartPlanning = request.scheduleApproved
            )
            isInstant || request.origin == Origin.DIRECT_OWNER_COMMAND -> Decision(
                lane = Lane.INSTANT,
                reason = if (isInstant) "explicit_immediate_request" else "direct_owner_command",
                mayStartPlanning = true
            )
            else -> Decision(
                lane = Lane.CUSTOM,
                reason = if (request.customScopeApproved) "owner_approved_custom_scope"
                    else "custom_scope_requires_owner_approval",
                mayStartPlanning = request.customScopeApproved
            )
        }
    }

    private const val MAX_ROUTING_TEXT = 12_000
}
