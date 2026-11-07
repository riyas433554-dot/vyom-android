package com.hermes.assistant

/**
 * Executes a bounded sequence of individually gated runner attempts.
 * The runner remains responsible for fresh snapshots, leases, approval and verification.
 */
internal object VyomFactoryDrainCoordinator {
    data class Result(
        val outcomes: List<VyomFactoryExecutionRunner.Outcome>,
        val reachedLimit: Boolean
    )

    fun drain(
        runNext: () -> VyomFactoryExecutionRunner.Outcome,
        shouldStop: () -> Boolean,
        maxTasks: Int = VyomFactoryDrainPolicy.MAX_TASKS_PER_FOREGROUND_RUN,
        onOutcome: (VyomFactoryExecutionRunner.Outcome) -> Unit = {}
    ): Result {
        require(maxTasks > 0) { "maxTasks must be positive" }
        val outcomes = mutableListOf<VyomFactoryExecutionRunner.Outcome>()
        while (outcomes.size < maxTasks && !shouldStop()) {
            val outcome = runNext()
            outcomes += outcome
            onOutcome(outcome)
            if (!VyomFactoryDrainPolicy.shouldContinue(outcome)) break
        }
        return Result(
            outcomes = outcomes.toList(),
            reachedLimit = outcomes.size == maxTasks &&
                outcomes.lastOrNull()?.let(VyomFactoryDrainPolicy::shouldContinue) == true
        )
    }
}
