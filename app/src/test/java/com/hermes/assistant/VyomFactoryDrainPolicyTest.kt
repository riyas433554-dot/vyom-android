package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VyomFactoryDrainPolicyTest {
    @Test fun advancesAfterVerifiedSuccessAndRecoverableFailure() {
        assertEquals(true, VyomFactoryDrainPolicy.shouldContinue(
            VyomFactoryExecutionRunner.Outcome.Succeeded(1, "attempt", "verified", true)
        ))
        assertEquals(true, VyomFactoryDrainPolicy.shouldContinue(
            VyomFactoryExecutionRunner.Outcome.Failed(2, "attempt", "execution_error")
        ))
    }

    @Test fun advancesAfterNonTerminalSkipButNotAfterStop() {
        assertEquals(true, VyomFactoryDrainPolicy.shouldContinue(
            VyomFactoryExecutionRunner.Outcome.Skipped(3, "confirmation_declined_or_unavailable")
        ))
        assertEquals(false, VyomFactoryDrainPolicy.shouldContinue(
            VyomFactoryExecutionRunner.Outcome.Skipped(4, "stopped_before_execution")
        ))
        assertEquals(false, VyomFactoryDrainPolicy.shouldContinue(
            VyomFactoryExecutionRunner.Outcome.Failed(5, "attempt", "stopped")
        ))
    }

    @Test fun stopsAfterSharedInfrastructureFailures() {
        listOf("permission_denied", "rate_limited", "server_unavailable", "network_timeout", "github_rejected")
            .forEach { category ->
                assertEquals(false, VyomFactoryDrainPolicy.shouldContinue(
                    VyomFactoryExecutionRunner.Outcome.Failed(7, "attempt", category)
                ))
            }
    }

    @Test fun stopsOnFenceBusyOrNoWork() {
        assertEquals(false, VyomFactoryDrainPolicy.shouldContinue(
            VyomFactoryExecutionRunner.Outcome.Fenced(6)
        ))
        assertEquals(false, VyomFactoryDrainPolicy.shouldContinue(
            VyomFactoryExecutionRunner.Outcome.Busy
        ))
        assertEquals(false, VyomFactoryDrainPolicy.shouldContinue(
            VyomFactoryExecutionRunner.Outcome.NoWork
        ))
    }

    @Test fun drainHasAnExplicitBound() {
        assertEquals(20, VyomFactoryDrainPolicy.MAX_TASKS_PER_FOREGROUND_RUN)
    }
}
