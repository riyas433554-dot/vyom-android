package com.hermes.assistant

internal interface VyomFactoryExecutionControl {
    /** Adapters must check before each side effect and while waiting on long work. */
    fun shouldStop(): Boolean
}

internal interface VyomFactoryExecutionAdapter {
    val target: VyomFactoryExecutionEnvelope.Target
    val isIdempotent: Boolean
    /** Empty only for legacy/test adapters; production adapters declare typed operations. */
    val capabilities: List<VyomFactoryCapabilityRegistry.Capability> get() = emptyList()
    /** True only when the adapter independently verified the requested postcondition. */
    fun isVerifiedResult(result: String): Boolean
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

    fun supports(envelope: VyomFactoryExecutionEnvelope): Boolean {
        if (!envelope.isExecutable || envelope.operation == null) return false
        val adapter = byTarget[envelope.target] ?: return false
        if (adapter.capabilities.isEmpty()) return true
        val domain = when (envelope.target) {
            VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION ->
                VyomFactoryCapabilityRegistry.Domain.ANDROID_CONTROL
            VyomFactoryExecutionEnvelope.Target.REPOSITORY_CHANGE ->
                VyomFactoryCapabilityRegistry.Domain.REPOSITORY
            VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW -> return false
        }
        val registry = VyomFactoryCapabilityRegistry(adapter.capabilities)
        return registry.discover(domain, envelope.operation).isNotEmpty()
    }

    fun isIdempotent(target: VyomFactoryExecutionEnvelope.Target): Boolean =
        byTarget[target]?.isIdempotent == true

    fun isVerifiedResult(target: VyomFactoryExecutionEnvelope.Target, result: String): Boolean =
        byTarget[target]?.isVerifiedResult(result) == true

    fun dispatch(
        envelope: VyomFactoryExecutionEnvelope,
        control: VyomFactoryExecutionControl
    ): Outcome {
        if (!envelope.isExecutable ||
            envelope.target == VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW
        ) return Outcome.ManualReview

        val adapter = byTarget[envelope.target] ?: return Outcome.Unsupported(envelope.target)
        if (!supports(envelope)) return Outcome.Unsupported(envelope.target)
        if (control.shouldStop()) throw VyomFactoryExecutionStoppedException()
        return Outcome.Executed(adapter.run(envelope, control))
    }
}

internal class VyomFactoryExecutionStoppedException : Exception("Execution stop requested")
