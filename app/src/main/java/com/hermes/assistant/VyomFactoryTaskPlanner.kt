package com.hermes.assistant

/**
 * Capability-aware first planning boundary.
 *
 * This deliberately stops at candidate discovery: it does not invent an
 * operation, construct executable arguments, or dispatch a specialist.
 */
internal class VyomFactoryTaskPlanner(
    private val classifier: (VyomFactoryGitHubClient.Task) -> VyomFactoryTaskClassifier.Result =
        { task -> VyomFactoryTaskClassifier.classify(task) }
) {
    sealed class Plan {
        data class CapabilityCandidates(
            val domain: VyomFactoryCapabilityRegistry.Domain,
            val capabilityIds: List<String>
        ) : Plan()

        data class BlockedCapability(
            val domain: VyomFactoryCapabilityRegistry.Domain,
            val reason: String
        ) : Plan()

        data class NeedsOwnerClarification(val reason: String) : Plan()
    }

    fun plan(
        task: VyomFactoryGitHubClient.Task,
        capabilities: VyomFactoryCapabilityRegistry
    ): Plan = when (val classification = classifier(task)) {
        is VyomFactoryTaskClassifier.Result.NeedsOwnerClarification ->
            Plan.NeedsOwnerClarification(classification.reason)

        is VyomFactoryTaskClassifier.Result.Classified -> {
            val matching = capabilities.all().filter { it.domain == classification.domain }
            val available = matching.filter { it.available }.map { it.id }
            when {
                available.isNotEmpty() ->
                    Plan.CapabilityCandidates(classification.domain, available)
                matching.isEmpty() ->
                    Plan.BlockedCapability(classification.domain, "no_registered_capability")
                else ->
                    Plan.BlockedCapability(
                        classification.domain,
                        matching.mapNotNull { it.unavailableReason }.distinct().sorted().joinToString("; ")
                            .ifBlank { "capability_unavailable" }
                    )
            }
        }
    }
}
