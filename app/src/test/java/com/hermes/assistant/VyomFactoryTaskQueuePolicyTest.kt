package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryTaskQueuePolicyTest {
    @Test fun priorityOrderIsStableWithinPriority() {
        val tasks = listOf(
            task(4, "P2", 1), task(3, "P0", 9), task(2, "P1", 3),
            task(1, "P1", 2), task(5, "P3", 0)
        )
        assertEquals(listOf(3, 1, 2, 4, 5), VyomFactoryTaskQueuePolicy.order(tasks).map { it.issueNumber })
    }

    @Test fun duplicateIssueOrSourceIdentityIsRejectedByPolicy() {
        val first = task(10, "P2", 1)
        assertTrue(VyomFactoryTaskQueuePolicy.isUnique(listOf(first), task(11, "P1", 2)))
        assertFalse(VyomFactoryTaskQueuePolicy.isUnique(listOf(first), task(10, "P0", 3)))
        assertFalse(VyomFactoryTaskQueuePolicy.isUnique(listOf(first), task(12, "P0", 3)))
    }

    private fun task(issue: Int, priority: String, time: Long) =
        VyomFactoryTaskStore.QueuedTask(issue, "Task $issue", priority, "source-$time", time)
}
