package com.hermes.assistant

/** Converts non-chat worker infrastructure failures into stable, privacy-safe Factory intake records. */
internal object VyomFactoryWorkerFailureRouter {
    data class Report(val sourceId: String, val complaint: String)

    private val workers = setOf("factory_runner", "skill_repository")
    private val failureTypes = setOf("IOException", "SocketTimeoutException", "UnknownHostException", "ConnectException", "SSLException", "IllegalStateException", "IllegalArgumentException", "SecurityException", "FactoryApiException", "MalformedInputException", "FileNotFoundException", "RuntimeException", "Exception", "UnknownFailure")

    fun create(worker: String, taskId: String, failureType: String, failureMessage: String? = null, topFrame: String? = null): Report {
        require(worker in workers) { "Unsupported non-chat worker" }
        val stableTaskId = taskId.trim()
        require(stableTaskId.isNotEmpty() && stableTaskId.length <= 80 &&
            stableTaskId.none { it.isISOControl() }) { "Worker task identity is invalid" }
        val safeFailureType = failureType.trim().takeIf { it in failureTypes } ?: "UnknownFailure"
        val sourceId = "$worker:$stableTaskId"
        require(sourceId.length <= 90) // Leaves room for the persisted task-failure prefix within the 120-character source-ID contract.
        val diagnostics = if (failureMessage != null || topFrame != null) {
            val safeMessage = VyomAuditPolicy.sanitize(failureMessage)
            val safeFrame = topFrame?.trim()?.takeIf {
                Regex("""^[A-Za-z0-9_.$-]{1,120}\.(kt|java):[1-9][0-9]{0,5}$""").matches(it)
            } ?: "unavailable"
            " Message: $safeMessage. Source: $safeFrame."
        } else ""
        val complaint = "Vyom Factory worker infrastructure failure. Worker: $worker. Task reference: $stableTaskId. " +
            "Failure type: $safeFailureType.$diagnostics Original request and credentials remain withheld."
        return Report(sourceId, complaint)
    }
}
