package com.hermes.assistant

/**
 * Conservative, deterministic first-pass domain classifier.
 *
 * It never chooses an executable operation. Ambiguous or unrecognized issues
 * are routed to owner clarification rather than guessed into a specialist.
 */
internal object VyomFactoryTaskClassifier {
    sealed class Result {
        data class Classified(val domain: VyomFactoryCapabilityRegistry.Domain) : Result()
        data class NeedsOwnerClarification(val reason: String) : Result()
    }

    private val signals = linkedMapOf(
        VyomFactoryCapabilityRegistry.Domain.ERROR_DIAGNOSTICS to Regex(
            "\\b(crash(?:es|ed)?|exception|stack\\s*trace|logcat|anr|fatal|error|diagnostic)\\b",
            RegexOption.IGNORE_CASE
        ),
        VyomFactoryCapabilityRegistry.Domain.REPOSITORY to Regex(
            "\\b(code|compile|compilation|gradle|build|ci|pull\\s*request|repository|repo|git)\\b",
            RegexOption.IGNORE_CASE
        ),
        VyomFactoryCapabilityRegistry.Domain.DESIGN_UI to Regex(
            "\\b(ui|design|layout|screen|button|theme|visual|animation)\\b",
            RegexOption.IGNORE_CASE
        ),
        VyomFactoryCapabilityRegistry.Domain.ANDROID_CONTROL to Regex(
            "\\b(volume|brightness|wi-?fi|bluetooth|flashlight|airplane\\s*mode|rotate|screenshot)\\b",
            RegexOption.IGNORE_CASE
        ),
        VyomFactoryCapabilityRegistry.Domain.MEMORY to Regex(
            "\\b(memory|remember|forget|recall|stored\\s+fact|conflict\\s+resolution)\\b",
            RegexOption.IGNORE_CASE
        ),
        VyomFactoryCapabilityRegistry.Domain.SKILLS to Regex(
            "\\b(skill|import\\s+skill|skill\\s+repository)\\b",
            RegexOption.IGNORE_CASE
        )
    )

    fun classify(task: VyomFactoryGitHubClient.Task): Result {
        val cleanBody = task.body
            .replace(Regex("<!--\\s*VYOM_FACTORY_META:.*?\\s*-->", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("<!--\\s*VYOM_FACTORY_EXECUTION:.*?\\s*-->", RegexOption.DOT_MATCHES_ALL), " ")
        val text = "${task.title}\n${cleanBody}".take(MAX_CLASSIFICATION_TEXT)
        val matches = signals.filterValues { it.containsMatchIn(text) }.keys
        return when (matches.size) {
            0 -> Result.NeedsOwnerClarification("no_domain_signal")
            1 -> Result.Classified(matches.single())
            else -> Result.NeedsOwnerClarification("multiple_domains")
        }
    }

    private const val MAX_CLASSIFICATION_TEXT = 12_000
}
