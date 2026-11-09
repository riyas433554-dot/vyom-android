package com.hermes.assistant

/**
 * Fail-closed policy for a repository worker change set.
 *
 * A plan carries no repository URL or credentials: both are resolved from the
 * owner-approved system registry. This validates a proposal only; it does not
 * execute commands, write a repository, or open a pull request.
 */
internal object VyomFactoryRepositoryChangePolicy {
    data class Change(
        val path: String,
        val content: String
    )

    data class Plan(
        val systemId: String,
        val issueNumber: Int,
        val attemptId: String,
        val title: String,
        val baseBranch: String,
        val baseCommitSha: String,
        val changes: List<Change>,
        val ownerApproved: Boolean
    )

    const val MAX_FILES = 20
    const val MAX_FILE_CONTENT_CHARS = 200_000
    const val MAX_TOTAL_CONTENT_CHARS = 500_000

    private val protectedFileNames = setOf(
        "MainActivity.kt",
        "ActionEngine.kt",
        "DeviceActionExecutor.kt",
        "VyomAccessibilityService.kt",
        "VyomFactoryExecutionAdapterRegistry.kt",
        "VyomFactoryExecutionEnvelope.kt",
        "VyomFactoryExecutionJournal.kt",
        "VyomFactoryExecutionRunner.kt",
        "VyomFactoryGitHubClient.kt",
        "VyomFactorySystemRegistry.kt",
        "VyomFactoryWorkerClaimCoordinator.kt",
        "VyomFactoryWorkerLeaseStore.kt",
        "VyomFactoryWorkerSession.kt",
        "SelfModificationClient.kt",
        "SelfModificationPolicy.kt",
        "SigningConfig.kt",
        "ReleaseSigning.kt",
        "UpdateVerifier.kt",
        "VyomUpdateClient.kt",
        "VyomShizukuShellClient.kt",
        "VyomShizukuUserService.kt",
        "AndroidManifest.xml",
        "build.gradle",
        "build.gradle.kts",
        "settings.gradle",
        "settings.gradle.kts",
        "gradle.properties",
        "local.properties",
        "gradlew",
        "gradlew.bat",
        "gradle-wrapper.properties",
        "gradle-wrapper.jar",
        ".gitmodules",
        "Dockerfile",
        "Makefile",
        "package.json",
        "package-lock.json",
        "yarn.lock",
        "pnpm-lock.yaml",
        "requirements.txt",
        "pyproject.toml",
        "Cargo.toml",
        "Cargo.lock",
        "go.mod",
        "go.sum",
        "Gemfile",
        "composer.json"
    )

    /**
     * Returns the exact registered binding only when the whole plan is safe.
     * Every target path is checked independently against the same immutable SHA.
     */
    fun validate(
        plan: Plan,
        registry: VyomFactorySystemRegistry
    ): VyomFactorySystemRegistry.Binding {
        if (!plan.ownerApproved) throw SecurityException("Owner approval is required for repository changes")
        return validateCandidate(plan, registry)
    }

    /** Validates untrusted proposed content without treating issue fields as approval. */
    fun validateCandidate(
        plan: Plan,
        registry: VyomFactorySystemRegistry
    ): VyomFactorySystemRegistry.Binding {
        require(plan.systemId.matches(Regex("^[a-z][a-z0-9_.-]{0,63}$"))) {
            "Invalid repository worker system ID"
        }
        require(plan.issueNumber > 0) { "Repository worker issue number must be positive" }
        require(plan.attemptId.matches(Regex("^[A-Za-z0-9][A-Za-z0-9-]{7,79}$"))) {
            "Invalid repository worker attempt ID"
        }
        require(plan.title.trim().length in 8..120) {
            "Repository change title must be 8–120 characters"
        }
        validateBranchName(plan.baseBranch)

        val binding = registry.get(plan.systemId)
            ?: throw SecurityException("Repository worker has no owner-approved system binding")
        if (plan.baseCommitSha != binding.commitSha) {
            throw SecurityException("Repository worker base SHA differs from the immutable system binding")
        }
        val parsed = VyomFactoryRepositoryIntake.parseUrl(binding.repositoryUrl)
            ?: throw SecurityException("Repository worker binding has an unsupported repository URL")
        if (parsed.owner == "riyas433554-dot" && parsed.name == "vyom-android" &&
            plan.baseBranch in setOf("main", "master")
        ) {
            throw SecurityException("Vyom Android main/master branches are protected from Factory worker changes")
        }

        require(plan.changes.isNotEmpty()) { "Repository worker change set is empty" }
        require(plan.changes.size <= MAX_FILES) { "Repository worker change set exceeds $MAX_FILES files" }
        val paths = plan.changes.map { it.path }
        require(paths.distinct().size == paths.size) { "Repository worker change set contains duplicate paths" }

        var totalChars = 0L
        plan.changes.forEach { change ->
            val authorized = registry.requireAuthorizedBinding(
                systemId = plan.systemId,
                relativePath = change.path,
                repositoryUrl = binding.repositoryUrl,
                commitSha = plan.baseCommitSha
            )
            if (authorized != binding) {
                throw SecurityException("Repository worker binding changed while validating the plan")
            }
            if (isProtectedPath(change.path)) {
                throw SecurityException("Repository worker policy protects this path from automated changes")
            }
            require(change.content.isNotEmpty()) { "Repository worker cannot write empty file content" }
            require(change.content.length <= MAX_FILE_CONTENT_CHARS) {
                "Repository worker file content exceeds $MAX_FILE_CONTENT_CHARS characters"
            }
            require('\u0000' !in change.content) { "Binary content is not accepted by the repository worker" }
            totalChars += change.content.length
        }
        require(totalChars <= MAX_TOTAL_CONTENT_CHARS) {
            "Repository worker change set exceeds the total content limit"
        }
        return binding
    }

    private fun validateBranchName(branch: String) {
        require(branch.length in 1..100 &&
            Regex("^[A-Za-z0-9][A-Za-z0-9._/-]*$").matches(branch) &&
            !branch.contains("..") &&
            !branch.contains("//") &&
            !branch.endsWith("/") &&
            !branch.endsWith(".lock") &&
            !branch.startsWith("refs/")
        ) { "Invalid repository worker base branch" }
    }

    private fun isProtectedPath(path: String): Boolean {
        val normalized = path.lowercase()
        val fileName = path.substringAfterLast('/')
        if (fileName in protectedFileNames) return true
        val segments = normalized.split("/")
        if (segments.any { it in setOf(".github", ".git", "gradle", "buildsrc", "build-logic", "scripts") }) {
            return true
        }
        if (normalized == ".env" || normalized.startsWith(".env.") ||
            segments.any { it in setOf("secrets", "credentials", "signing", "keystore") }
        ) return true
        if (normalized.endsWith(".gradle") || normalized.endsWith(".gradle.kts") ||
            normalized.endsWith(".sh") || normalized.endsWith(".bat") ||
            normalized.endsWith(".cmd") || normalized.endsWith(".ps1")
        ) return true
        return listOf(".jks", ".keystore", ".p12", ".pem", ".key", ".cer", ".crt", ".apk", ".aar", ".jar", ".so", ".dll", ".exe")
            .any(normalized::endsWith)
    }
}
