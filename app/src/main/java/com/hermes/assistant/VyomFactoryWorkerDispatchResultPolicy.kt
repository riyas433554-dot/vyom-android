package com.hermes.assistant

/** Converts dispatcher outcomes into truthful WorkManager terminal/retry states. */
internal object VyomFactoryWorkerDispatchResultPolicy {
    enum class Decision { SUCCESS, FAILURE, RETRY }

    fun decide(result: VyomFactoryWorkerPoolDispatcher.Result): Decision = when {
        result.stopped -> Decision.SUCCESS
        result.authorizationRequired -> Decision.FAILURE
        result.retryableFailureCount > 0 -> Decision.RETRY
        result.failedCount > 0 -> Decision.FAILURE
        else -> Decision.SUCCESS
    }
}
