package com.hermes.assistant

/** Canonical issue comments for terminal runner outcomes; never converts failure into success. */
internal object VyomFactoryExecutionAudit {
    fun comment(outcome: VyomFactoryExecutionRunner.Outcome): String? = when (outcome) {
        is VyomFactoryExecutionRunner.Outcome.Succeeded -> {
            val result = if (MemoryCapturePolicy.isSensitive(outcome.result)) "[redacted result]"
                else outcome.result.take(500)
            "[Factory execution VERIFIED] Issue #${outcome.issueNumber}; attempt ${outcome.attemptId}; " +
                "adapter postcondition verified: $result. The complaint remains open until its named complaint probe passes."
        }
        is VyomFactoryExecutionRunner.Outcome.Failed ->
            "[Factory execution FAILED] Issue #${outcome.issueNumber}; attempt ${outcome.attemptId}; " +
                "category=${outcome.category}. No success was recorded; review the issue and retry policy."
        else -> null
    }
}
