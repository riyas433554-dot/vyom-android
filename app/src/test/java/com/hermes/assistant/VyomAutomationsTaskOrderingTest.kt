package com.hermes.assistant

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class VyomAutomationsTaskOrderingTest {
    @Test fun localTasksStayNewestFirstAndFactoryComplaintsKeepCanonicalPriorityOrder() {
        val local = listOf(task("local-older"), task("local-newer"))
        val factory = listOf(
            JSONObject().put("source", "factory").put("issueNumber", 12).put("priority", "P0"),
            JSONObject().put("source", "factory").put("issueNumber", 10).put("priority", "P0"),
            JSONObject().put("source", "factory").put("issueNumber", 8).put("priority", "P1")
        )

        val ordered = VyomAutomationsTaskOrdering.sort(local, factory)

        assertEquals(
            listOf("local-newer", "local-older", "factory-12", "factory-10", "factory-8"),
            ordered.map {
                if (it.optString("source") == "factory") "factory-${it.optInt("issueNumber")}"
                else it.optString("id")
            }
        )
    }

    @Test fun emptyFactoryInboxDoesNotChangeLocalTaskOrdering() {
        val local = listOf(task("older"), task("newer"))
        assertEquals(
            listOf("newer", "older"),
            VyomAutomationsTaskOrdering.sort(local, emptyList()).map { it.optString("id") }
        )
    }

    private fun task(id: String) = JSONObject().put("id", id).put("status", "queued")
}
