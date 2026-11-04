package com.hermes.assistant

import org.json.JSONObject

/** Keeps local task recency while preserving the canonical P0-first order of Factory complaints. */
internal object VyomAutomationsTaskOrdering {
    fun sort(localTasks: List<JSONObject>, factoryComplaints: List<JSONObject>): List<JSONObject> =
        localTasks.asReversed() + factoryComplaints
}
