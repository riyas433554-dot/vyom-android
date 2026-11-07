package com.hermes.assistant

import org.json.JSONObject

/**
 * Validated execution routing metadata embedded in a GitHub Issue body.
 *
 * This parser does not execute instructions. Missing, legacy, malformed, unknown,
 * or oversized envelopes are routed to manual review.
 */
internal data class VyomFactoryExecutionEnvelope(
    val target: Target,
    val originalRequest: String,
    val operation: String?,
    val arguments: JSONObject,
    val isExecutable: Boolean,
    val issueNumber: Int = 0
) {
    enum class Target {
        ANDROID_ACTION,
        REPOSITORY_CHANGE,
        MANUAL_REVIEW
    }

    companion object {
        const val MAX_BODY_LENGTH = 20_000
        private const val MAX_ARGUMENTS_LENGTH = 8_000
        private const val MAX_REQUEST_LENGTH = 12_000
        private const val MAX_OPERATION_LENGTH = 80
        private const val MARKER = "VYOM_FACTORY_EXECUTION:"
        private val markerPattern = Regex(
            "<!--\\s*$MARKER(.*?)\\s*-->",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
        private val operationPattern = Regex("[a-z][a-z0-9_]{1,79}")

        fun parse(body: String?, issueNumber: Int = 0): VyomFactoryExecutionEnvelope {
            val safeBody = body.orEmpty()
            val fallbackRequest = safeBody
                .replace(Regex("<!--\\s*VYOM_FACTORY_META:.*?\\s*-->", setOf(RegexOption.DOT_MATCHES_ALL)), "")
                .replace(markerPattern, "")
                .trim()
                .take(MAX_REQUEST_LENGTH)

            fun manualReview(request: String = fallbackRequest) = VyomFactoryExecutionEnvelope(
                target = Target.MANUAL_REVIEW,
                originalRequest = request,
                operation = null,
                arguments = JSONObject(),
                isExecutable = false,
                issueNumber = issueNumber
            )

            if (safeBody.isBlank() || safeBody.length > MAX_BODY_LENGTH) return manualReview()
            val markers = markerPattern.findAll(safeBody).toList()
            if (markers.size != 1) return manualReview()

            return runCatching {
                val json = JSONObject(markers.single().groupValues[1])
                if (json.optInt("schema", -1) != 1) return manualReview()
                val target = when (json.optString("target")) {
                    "android_action" -> Target.ANDROID_ACTION
                    "repository_change" -> Target.REPOSITORY_CHANGE
                    else -> return manualReview()
                }
                val request = json.optString("request").trim()
                if (request.isBlank() || request.length > MAX_REQUEST_LENGTH) return manualReview()
                val operation = when (target) {
                    Target.ANDROID_ACTION -> json.optString("action")
                    Target.REPOSITORY_CHANGE -> json.optString("operation")
                    Target.MANUAL_REVIEW -> return manualReview(request)
                }
                if (!operationPattern.matches(operation) || operation.length > MAX_OPERATION_LENGTH) {
                    return manualReview(request)
                }
                val arguments = json.optJSONObject("arguments") ?: return manualReview(request)
                if (arguments.toString().length > MAX_ARGUMENTS_LENGTH) return manualReview(request)
                VyomFactoryExecutionEnvelope(
                    target = target,
                    originalRequest = request,
                    operation = operation,
                    arguments = arguments,
                    isExecutable = true,
                    issueNumber = issueNumber
                )
            }.getOrElse { manualReview() }
        }
    }
}
