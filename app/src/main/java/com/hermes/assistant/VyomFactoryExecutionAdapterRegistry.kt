package com.hermes.assistant

internal interface VyomFactoryExecutionAdapter {
    val target: VyomFactoryExecutionEnvelope.Target
    fun run(envelope: VyomFactoryExecutionEnvelope): String
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

    fun dispatch(envelope: VyomFactoryExecutionEnvelope): Outcome {
        if (!envelope.isExecutable ||
            envelope.target == VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW
        ) return Outcome.ManualReview

        val adapter = byTarget[envelope.target] ?: return Outcome.Unsupported(envelope.target)
        return Outcome.Executed(adapter.run(envelope))
    }
}
