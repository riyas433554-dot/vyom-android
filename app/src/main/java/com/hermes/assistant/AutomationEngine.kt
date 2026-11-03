package com.hermes.assistant

import java.net.URI
import java.util.UUID

object AutomationEngine {
    enum class Risk { SAFE, CONFIRM, BLOCKED }
    enum class Decision { ALLOW, APPROVAL_REQUIRED, BLOCKED, INVALID }

    data class Step(
        val type: String,
        val target: String = "",
        val text: String = "",
        val durationMs: Int = 0,
        val x1: Float = 0f,
        val y1: Float = 0f,
        val x2: Float = 0f,
        val y2: Float = 0f
    )

    data class Macro(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val steps: List<Step>,
        val enabled: Boolean = true
    )

    data class Validation(val valid: Boolean, val errors: List<String>)

    private val allowedTypes = setOf(
        "open_app", "open_url", "tap_text", "type_text", "scroll",
        "swipe", "wait", "back", "home", "screenshot", "open_settings", "create_task"
    )
    private val settingsPanels = setOf("wifi", "bluetooth", "display", "sound", "apps", "notifications", "location", "accessibility")
    private val sensitiveTerms = Regex("(?i)\\b(bank|banking|payment|pay|upi|otp|password|passcode|pin|biometric|wallet|paypal|finance|trading|crypto|security)\\b|बैंक|भुगतान|ओटीपी|पासवर्ड|पिन")
        "(?i)\\b(bank|banking|बैंक|payment|भुगतान|pay|upi|otp|ओटीपी|password|पासवर्ड|passcode|pin|पिन|biometric|wallet|paypal|finance|trading|crypto|security)\\b"
    )
    private val consequentialTerms = Regex(
        "(?i)\\b(send|submit|delete|remove|install|purchase|buy|confirm|transfer|share|post|publish)\\b"
    )
    private val secretText = Regex(
        "(?i)(?:api[_-]?key|access[_-]?token|refresh[_-]?token|password|passcode|otp|pin|client[_-]?secret|private[_-]?key)\\s*[:=]\\s*\\S+|\\b(?:otp|password|passcode|pin)\\s+\\d{4,}"
    )

    fun validate(macro: Macro): Validation {
        val errors = mutableListOf<String>()
        if (runCatching { UUID.fromString(macro.id) }.isFailure) errors += "invalid_id"
        if (macro.name.trim().length !in 2..60) errors += "invalid_name"
        if (macro.steps.isEmpty() || macro.steps.size > 30) errors += "invalid_step_count"
        macro.steps.forEachIndexed { index, step ->
            if (step.type !in allowedTypes) {
                errors += "step_${index}_type_not_allowed"
                return@forEachIndexed
            }
            when (step.type) {
                "open_app" -> if (step.target.isBlank() || step.target.length > 80) errors += "step_${index}_invalid_app"
                "open_url" -> {
                    val uri = runCatching { URI(step.target) }.getOrNull()
                    if (uri?.scheme != "https" || uri.host.isNullOrBlank() || step.target.length > 2048) {
                        errors += "step_${index}_invalid_url"
                    }
                }
                "tap_text" -> if (step.target.isBlank() || step.target.length > 100) errors += "step_${index}_invalid_target"
                "type_text" -> {
                    if (step.text.isBlank() || step.text.length > 500) errors += "step_${index}_invalid_text"
                    if (secretText.containsMatchIn(step.text)) errors += "step_${index}_credential_input_blocked"
                }
                "scroll" -> if (step.target.lowercase() !in setOf("up", "down")) errors += "step_${index}_invalid_direction"
                "swipe" -> {
                    val coordinatesValid = listOf(step.x1, step.y1, step.x2, step.y2).all { it in 0f..1f }
                    if (!coordinatesValid || step.durationMs !in 100..1200) errors += "step_${index}_invalid_swipe"
                }
                "wait" -> if (step.durationMs !in 100..30000) errors += "step_${index}_invalid_wait"
                "open_settings" -> if (step.target.lowercase() !in settingsPanels) errors += "step_${index}_settings_panel_blocked"
                "create_task" -> {
                    if (step.text.isBlank() || step.text.length > 4000) errors += "step_${index}_invalid_task"
                    if (secretText.containsMatchIn(step.text)) errors += "step_${index}_credential_task_blocked"
                }
            }
        }
        return Validation(errors.isEmpty(), errors)
    }

    fun classify(macro: Macro): Risk {
        if (!validate(macro).valid) return Risk.BLOCKED
        val risks = macro.steps.map(::classifyStep)
        return when {
            risks.contains(Risk.BLOCKED) -> Risk.BLOCKED
            risks.contains(Risk.CONFIRM) -> Risk.CONFIRM
            else -> Risk.SAFE
        }
    }

    fun classifyStep(step: Step): Risk {
        val combined = "${step.target} ${step.text}"
        if (step.type != "create_task" && sensitiveTerms.containsMatchIn(combined)) return Risk.BLOCKED
        return when (step.type) {
            "type_text", "create_task", "swipe", "screenshot" -> Risk.CONFIRM
            "tap_text" -> if (consequentialTerms.containsMatchIn(step.target)) Risk.CONFIRM else Risk.SAFE
            "open_url" -> if (sensitiveTerms.containsMatchIn(step.target)) Risk.BLOCKED else Risk.SAFE
            "open_settings" -> if (step.target.lowercase() !in settingsPanels) Risk.BLOCKED else Risk.SAFE
            else -> Risk.SAFE
        }
    }

    fun decision(macro: Macro, ownerApproved: Boolean): Decision {
        if (!validate(macro).valid) return Decision.INVALID
        return when (classify(macro)) {
            Risk.BLOCKED -> Decision.BLOCKED
            Risk.CONFIRM -> if (ownerApproved) Decision.ALLOW else Decision.APPROVAL_REQUIRED
            Risk.SAFE -> Decision.ALLOW
        }
    }
}
