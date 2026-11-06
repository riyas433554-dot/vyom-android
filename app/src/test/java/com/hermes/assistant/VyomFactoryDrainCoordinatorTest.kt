package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryDrainCoordinatorTest {
    @Test fun continuesAfterRecordedOutcomesUntilRunnerReportsNoWork() {
        val pending = ArrayDeque(
            listOf(
                VyomFactoryExecutionRunner.Outcome.Skipped(1, "manual_review_required"),
                VyomFactoryExecutionRunner.Outcome.Failed(2, "attempt-2", "execution_error"),
                VyomFactoryExecutionRunner.Outcome.Succeeded(3, "attempt-3", "verified", true),
                VyomFactoryExecutionRunner.Outcome.NoWork
            )
        )
        val visited = mutableListOf<Int>()
        val result = VyomFactoryDrainCoordinator.drain(
            runNext = { pending.removeFirst() },
            shouldStop = { false },
            onOutcome = { outcome -> when (outcome) {
                is VyomFactoryExecutionRunner.Outcome.Succeeded -> visited.add(outcome.issueNumber)
                is VyomFactoryExecutionRunner.Outcome.Failed -> visited.add(outcome.issueNumber)
                is VyomFactoryExecutionRunner.Outcome.Skipped -> visited.add(outcome.issueNumber)
                is VyomFactoryExecutionRunner.Outcome.Fenced -> visited.add(outcome.issueNumber)
                else -> Unit
            } }
        )

        assertEquals(listOf(1, 2, 3), visited)
        assertEquals(4, result.outcomes.size)
        assertFalse(result.reachedLimit)
        assertTrue(pending.isEmpty())
    }

    @Test fun stopGatePreventsStartingTheNextTask() {
        val pending = ArrayDeque(
            listOf(
                VyomFactoryExecutionRunner.Outcome.Succeeded(1, "attempt-1", "verified", true),
                VyomFactoryExecutionRunner.Outcome.Succeeded(2, "attempt-2", "verified", true)
            )
        )
        var starts = 0
        val result = VyomFactoryDrainCoordinator.drain(
            runNext = { starts++; pending.removeFirst() },
            shouldStop = { starts >= 1 }
        )

        assertEquals(1, starts)
        assertEquals(1, result.outcomes.size)
        assertEquals(1, pending.size)
    }

    @Test fun sharedInfrastructureFailureEndsBatchWithoutClaimingLaterTasks() {
        var starts = 0
        val result = VyomFactoryDrainCoordinator.drain(
            runNext = {
                starts++
                VyomFactoryExecutionRunner.Outcome.Failed(1, "attempt", "rate_limited")
            },
            shouldStop = { false }
        )

        assertEquals(1, starts)
        assertEquals(1, result.outcomes.size)
    }

    @Test fun maxTaskBoundIsEnforced() {
        var next = 0
        val result = VyomFactoryDrainCoordinator.drain(
            runNext = {
                next++
                VyomFactoryExecutionRunner.Outcome.Succeeded(next, "attempt-$next", "verified", true)
            },
            shouldStop = { false },
            maxTasks = 3
        )

        assertEquals(3, next)
        assertEquals(3, result.outcomes.size)
        assertTrue(result.reachedLimit)
    }

    @Test fun fencedBusyAndStoppedOutcomesAreTerminal() {
        val terminal = listOf(
            VyomFactoryExecutionRunner.Outcome.Fenced(1),
            VyomFactoryExecutionRunner.Outcome.Busy,
            VyomFactoryExecutionRunner.Outcome.Skipped(2, "stopped_before_execution")
        )
        terminal.forEach { outcome ->
            var starts = 0
            val result = VyomFactoryDrainCoordinator.drain(
                runNext = {
                    starts++
                    outcome
                },
                shouldStop = { false }
            )
            assertEquals(1, starts)
            assertEquals(1, result.outcomes.size)
        }
    }
}
