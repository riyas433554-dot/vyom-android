package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspacePanelSummaryTest {
    @Test fun groupsTaskStatusesIntoWorkspaceCounts() {
        val summary = WorkspacePanelSummary.fromStatuses(
            listOf("queued", "running", "done", "completed", "error", "failed", "cancelled", "unknown")
        )
        assertEquals(2, summary.active)
        assertEquals(2, summary.completed)
        assertEquals(3, summary.errors)
    }

    @Test fun trimsAndNormalizesStatusValues() {
        val summary = WorkspacePanelSummary.fromStatuses(listOf(" RUNNING ", " SUCCESS "))
        assertEquals(1, summary.active)
        assertEquals(1, summary.completed)
        assertEquals(0, summary.errors)
    }
}