package com.hermes.assistant

/** Bounded foreground drain policy. Never advances after a fenced or stopped run. */
internal object VyomFactoryDrainPolicy {
    fun shouldContinue(outcome: VyomFactoryExecutionRunner.Outcome): Boolean = when (outcome) {
        is VyomFactoryExecutionRunner.Outcome.Succeeded -> true
        is VyomFactoryExecutionRunner.Outcome.Failed -> outcome.category != "stopped"
        is VyomFactoryExecutionRunner.Outcome.Skipped ->
            outcome.reason != "stopped_before_execution" && outcome.reason != "stopped"
        is VyomFactoryExecutionRunner.Outcome.Fenced,
        VyomFactoryExecutionRunner.Outcome.Busy,
        VyomFactoryExecutionRunner.Outcome.NoWork -> false
    }

    const val MAX_TASKS_PER_FOREGROUND_RUN = 20
}
