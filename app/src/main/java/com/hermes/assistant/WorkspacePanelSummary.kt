package com.hermes.assistant

internal data class WorkspacePanelSummary(
    val active: Int,
    val completed: Int,
    val errors: Int
) {
    companion object {
        fun fromStatuses(statuses: Iterable<String>): WorkspacePanelSummary {
            var active = 0
            var completed = 0
            var errors = 0
            statuses.forEach { raw ->
                when (raw.trim().lowercase()) {
                    "queued", "pending", "running", "in_progress", "processing", "working" -> active++
                    "done", "completed", "success" -> completed++
                    "error", "failed", "cancelled" -> errors++
                }
            }
            return WorkspacePanelSummary(active, completed, errors)
        }
    }
}