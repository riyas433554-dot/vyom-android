package com.hermes.assistant

/** Builds a bounded, privacy-checked task for the existing review-only self-change pipeline. */
internal object VyomFactorySelfChangePrompt {
    fun title(task: VyomFactoryGitHubClient.Task): String =
        "Fix Factory complaint #${task.number}: ${task.title}".take(120)

    fun build(task: VyomFactoryGitHubClient.Task): String {
        val description = task.body.substringBefore("<!-- VYOM_FACTORY_META:").trim()
        require(!MemoryCapturePolicy.isSensitive(task.title) && !MemoryCapturePolicy.isSensitive(description)) {
            "Sensitive complaint title or text cannot be sent to the code-generation model"
        }
        val prompt = buildString {
            appendLine("Fix Factory complaint #${task.number}: ${task.title}")
            appendLine("Issue: https://github.com/riyas433554-dot/vyom-android/issues/${task.number}")
            appendLine("Priority: ${task.metadata.priority}")
            appendLine("Complaint details:")
            appendLine(description.take(1_300))
            append("Create the smallest safe one-file Kotlin change that addresses this complaint. Preserve protected policy, STOP, audit, signing, update, accessibility and control files. If the correct file is protected or the issue needs multiple files, explain that and do not force a patch.")
        }
        require(prompt.length <= 2_000) { "Factory complaint context exceeds the safe proposal limit" }
        return prompt
    }
}
