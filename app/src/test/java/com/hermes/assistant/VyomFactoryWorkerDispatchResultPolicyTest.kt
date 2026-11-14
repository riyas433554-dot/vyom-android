package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VyomFactoryWorkerDispatchResultPolicyTest {
    @Test fun nonRetryableWorkerFailureIsNotReportedAsSuccess() {
        val result = VyomFactoryWorkerPoolDispatcher.Result(
            outcomes = listOf(
                VyomFactoryWorkerPoolDispatcher.WorkerOutcome(
                    workerId = "instant-1",
                    issueNumber = 101,
                    state = VyomFactoryWorkerPoolDispatcher.State.FAILED,
                    errorType = "SecurityException",
                    retryable = false
                )
            )
        )

        assertEquals(VyomFactoryWorkerDispatchResultPolicy.Decision.FAILURE,
            VyomFactoryWorkerDispatchResultPolicy.decide(result))
    }

    @Test fun retryableWorkerFailureRequestsRetry() {
        val result = VyomFactoryWorkerPoolDispatcher.Result(
            outcomes = listOf(
                VyomFactoryWorkerPoolDispatcher.WorkerOutcome(
                    workerId = "scheduled-1",
                    issueNumber = 102,
                    state = VyomFactoryWorkerPoolDispatcher.State.FAILED,
                    errorType = "IOException",
                    retryable = true
                )
            )
        )

        assertEquals(VyomFactoryWorkerDispatchResultPolicy.Decision.RETRY,
            VyomFactoryWorkerDispatchResultPolicy.decide(result))
    }

    @Test fun stoppedDispatchDoesNotBecomeFailure() {
        val result = VyomFactoryWorkerPoolDispatcher.Result(emptyList(), stopped = true)

        assertEquals(VyomFactoryWorkerDispatchResultPolicy.Decision.SUCCESS,
            VyomFactoryWorkerDispatchResultPolicy.decide(result))
    }

    @Test fun authorizationRequiredIsFailureRatherThanNoWorkSuccess() {
        val result = VyomFactoryWorkerPoolDispatcher.Result(emptyList(), authorizationRequired = true)

        assertEquals(VyomFactoryWorkerDispatchResultPolicy.Decision.FAILURE,
            VyomFactoryWorkerDispatchResultPolicy.decide(result))
    }
}
