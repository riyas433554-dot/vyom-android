package com.hermes.assistant

/**
 * Guardrail for the in-app self-change proposal workflow.
 * Proposals can only edit Kotlin source files under the app package. Safety policy,
 * control dispatch, kill-switch, audit and update code are deliberately excluded.
 * Every proposal is opened as a PR against the working branch; this class never merges.
 */
internal object SelfModificationPolicy {
    const val PROPOSAL_BASE_BRANCH = "feature/real-worker-model"
    private const val MAX_TITLE_LENGTH = 120
    private const val MAX_CONTENT_LENGTH = 200_000

    private val protectedRepositoryPaths = setOf("config/release-certificate.sha256")

    private val protectedFileNames = setOf(
        "MainActivity.kt",
        "ActionEngine.kt",
        "DeviceActionExecutor.kt",
        "VoiceForegroundService.kt",
        "VyomAccessibilityService.kt",
        "SelfModificationPolicy.kt",
        "SelfModificationClient.kt",
        "UpdateVerifier.kt",
        "VyomUpdateClient.kt",
        "SigningConfig.kt",
        "ReleaseSigning.kt",
        "UiBundleEditor.kt",
        "UiBundleStore.kt",
        "UiBundleIntegrity.kt",
        "VyomShizukuShellClient.kt",
        "VyomShizukuUserService.kt"
    )

    fun isExplicitlyProtectedRepositoryPath(path: String): Boolean =
        path in protectedRepositoryPaths

    fun isAllowedPath(path: String): Boolean {
        if (isExplicitlyProtectedRepositoryPath(path)) return false
        if (path.length > 240 || path.contains("..") || path.contains("//")) return false
        if (!Regex("^app/src/main/java/com/hermes/assistant/[A-Za-z0-9_/-]+\\.kt$").matches(path)) return false
        val fileName = path.substringAfterLast('/')
        if (fileName in protectedFileNames) return false
        val lower = path.lowercase()
        if (listOf("policy", "audit", "kill", "control", "accessibilityservice", "deviceactionexecutor",
                "voiceforegroundservice", "selfmodification", "update", "signing", "keystore", "uibundle", "shizuku").any { it in lower }) return false
        return true
    }

    /**
     * Self-modification is a separate Factory system: both the registry boundary
     * and this narrower source-file policy must authorize every target path.
     */
    fun requireSystemBinding(
        registry: VyomFactorySystemRegistry,
        systemId: String,
        repositoryUrl: String,
        commitSha: String,
        path: String
    ): VyomFactorySystemRegistry.Binding {
        require(isAllowedPath(path)) {
            "Self-modification policy rejected this source path"
        }
        return registry.requireAuthorizedBinding(
            systemId = systemId,
            relativePath = path,
            repositoryUrl = repositoryUrl,
            commitSha = commitSha
        )
    }

    fun validateDiscoveryTask(title: String, task: String) {
        require(title.trim().length in 8..MAX_TITLE_LENGTH) { "Proposal title must be 8–120 characters" }
        require(task.isNotBlank()) { "Describe the code change before generating a proposal" }
        require(task.length <= 2_000) { "Self-update task description exceeds 2,000 characters" }
        require(!MemoryCapturePolicy.isSensitive(title) && !MemoryCapturePolicy.isSensitive(task)) {
            "Sensitive credentials or personal data cannot be sent to the code-generation model"
        }
    }

    fun validateTask(title: String, path: String, task: String) {
        validateDiscoveryTask(title, task)
        require(isAllowedPath(path)) { "This file is protected or outside the allowed Kotlin source area" }
    }

    fun validate(title: String, path: String, content: String) {
        require(title.trim().length in 8..MAX_TITLE_LENGTH) { "Proposal title must be 8–120 characters" }
        require(isAllowedPath(path)) { "This file is protected or outside the allowed Kotlin source area" }
        require(content.isNotBlank()) { "Proposed file content cannot be blank" }
        require(content.length <= MAX_CONTENT_LENGTH) { "Proposed file content exceeds 200 KB" }
    }
}
