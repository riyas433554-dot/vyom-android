package com.hermes.assistant

internal interface VyomFactoryExecutionControl {
    /** Adapters must check before each side effect and while waiting on long work. */
    fun shouldStop(): Boolean
}

internal interface VyomFactoryExecutionAdapter {
    val target: VyomFactoryExecutionEnvelope.Target
    val isIdempotent: Boolean
    fun run(envelope: VyomFactoryExecutionEnvelope, control: VyomFactoryExecutionControl): String
}

internal class VyomFactoryExecutionAdapterRegistry(
    adapters: List<VyomFactoryExecutionAdapter>
) {
    sealed class Outcome {
        data class Executed(val result: String) : Outcome()
        object ManualReview : Outcome()
        data class Unsupported(val target: VyomFactoryExecutionEnvelope.Target) : Outcome()
    }

    private val byTarget: Map<VyomFactoryExecutionEnvelope.Target, VyomFactoryExecutionAdapter>

    init {
        require(adapters.none { it.target == VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW })
        require(adapters.map { it.target }.distinct().size == adapters.size)
        byTarget = adapters.associateBy { it.target }
    }

    fun supports(target: VyomFactoryExecutionEnvelope.Target): Boolean = target in byTarget

    fun isIdempotent(target: VyomFactoryExecutionEnvelope.Target): Boolean =
        byTarget[target]?.isIdempotent == true

    fun dispatch(
        envelope: VyomFactoryExecutionEnvelope,
        control: VyomFactoryExecutionControl
    ): Outcome {
        if (!envelope.isExecutable ||
            envelope.target == VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW
        ) return Outcome.ManualReview

        val adapter = byTarget[envelope.target] ?: return Outcome.Unsupported(envelope.target)
        if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
        return Outcome.Executed(adapter.run(envelope, control))
    }
}

internal class VyomFactoryExecutionStoppedException : Exception("Execution stop requested")
