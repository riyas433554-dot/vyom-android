package com.hermes.assistant

/** Keeps the worker-planning permission tied to an explicit owner START/RESUME action. */
internal object VyomFactoryPlanningApprovalPolicy {
    fun requestedLanePlanningAllowed(
        currentStatus: String,
        nextStatus: String,
        ownerApprovedStart: Boolean
    ): Boolean? {
        if (ownerApprovedStart) {
            require(nextStatus == "in_progress" && currentStatus in setOf("open", "paused")) {
                "Planning approval requires an explicit START or RESUME transition"
            }
            return true
        }
        if (nextStatus != currentStatus &&
            nextStatus in setOf("open", "paused", "blocked", "completed", "cancelled")
        ) return false
        return null
    }
}
