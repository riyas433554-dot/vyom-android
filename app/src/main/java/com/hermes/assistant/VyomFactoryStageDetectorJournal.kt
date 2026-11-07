package com.hermes.assistant

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Persistent local audit journal for planning, implementation and test detector outcomes. */
internal class VyomFactoryStageDetectorJournal private constructor(
    private val prefs: SharedPreferences
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    )

    fun record(event: VyomFactoryStageDetector.Event) {
        synchronized(LOCK) {
            val events = readAll().toMutableList()
            events += event
            val array = JSONArray()
            events.takeLast(MAX_EVENTS).forEach { item ->
                array.put(JSONObject()
                    .put("parent_issue_number", item.parentIssueNumber)
                    .put("stage", item.stage.name)
                    .put("checkpoint", item.checkpoint)
                    .put("outcome", item.outcome.name)
                    .put("finding_count", item.findingCount)
                    .put("created_issue_numbers", JSONArray(item.createdIssueNumbers))
                    .put("duplicate_count", item.duplicateCount)
                    .put("detail", item.detail)
                    .put("recorded_at", item.recordedAt))
            }
            check(prefs.edit().putString(KEY_EVENTS, array.toString()).commit()) {
                "Stage detector journal could not be persisted"
            }
        }
    }

    fun all(): List<VyomFactoryStageDetector.Event> = synchronized(LOCK) { readAll() }

    private fun readAll(): List<VyomFactoryStageDetector.Event> {
        val raw = prefs.getString(KEY_EVENTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            require(array.length() <= MAX_EVENTS)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val ids = item.getJSONArray("created_issue_numbers")
                VyomFactoryStageDetector.Event(
                    parentIssueNumber = item.getInt("parent_issue_number"),
                    stage = VyomFactoryTaskIntelligence.Stage.valueOf(item.getString("stage")),
                    checkpoint = item.getString("checkpoint"),
                    outcome = VyomFactoryStageDetector.Outcome.valueOf(item.getString("outcome")),
                    findingCount = item.getInt("finding_count"),
                    createdIssueNumbers = (0 until ids.length()).map { ids.getInt(it) },
                    duplicateCount = item.getInt("duplicate_count"),
                    detail = item.getString("detail"),
                    recordedAt = item.getLong("recorded_at")
                )
            }
        } catch (error: Exception) {
            throw IllegalStateException("Stage detector journal is unreadable; refusing to overwrite it", error)
        }
    }

    companion object {
        private const val PREFS = "vyom_factory_stage_detector_journal"
        private const val KEY_EVENTS = "events_v1"
        private const val MAX_EVENTS = 500
        private val LOCK = Any()
    }
}
