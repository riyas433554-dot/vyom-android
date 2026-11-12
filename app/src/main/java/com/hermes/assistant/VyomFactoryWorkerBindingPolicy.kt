package com.hermes.assistant

/** Owner-approved binding for the isolated Factory code worker. */
internal object VyomFactoryWorkerBindingPolicy {
    const val SYSTEM_ID = "worker"
    const val TRUSTED_REPOSITORY_URL = "https://github.com/riyas433554-dot/vyom-android"
    const val MAIN_SOURCE_ROOT = "app/src/main/java/com/hermes/assistant"
    const val TEST_SOURCE_ROOT = "app/src/test/java/com/hermes/assistant"
    val ALLOWED_PATHS = listOf(MAIN_SOURCE_ROOT, TEST_SOURCE_ROOT)

    fun createBinding(repositoryUrl: String, baseSha: String, registeredAtEpochMs: Long): VyomFactorySystemRegistry.Binding {
        require(repositoryUrl == repositoryUrl.trim()) { "Repository URL must not contain surrounding whitespace" }
        val source = VyomFactoryRepositoryIntake.parseUrl(repositoryUrl)
            ?: throw IllegalArgumentException("Enter the trusted Vyom Android GitHub repository URL")
        require(source.canonicalUrl == repositoryUrl && source.canonicalUrl == TRUSTED_REPOSITORY_URL) {
            "Factory workers are restricted to $TRUSTED_REPOSITORY_URL"
        }
        require(baseSha.matches(Regex("^[a-f0-9]{40}$"))) {
            "Base SHA must be the exact 40-character lowercase hexadecimal commit"
        }
        require(registeredAtEpochMs > 0L)
        return VyomFactorySystemRegistry.Binding(
            systemId = SYSTEM_ID,
            repositoryUrl = source.canonicalUrl,
            commitSha = baseSha,
            allowedPaths = ALLOWED_PATHS,
            registeredAtEpochMs = registeredAtEpochMs
        )
    }

    fun allowsPath(path: String): Boolean =
        ALLOWED_PATHS.any { path.startsWith("$it/") } && SelfModificationPolicy.isAllowedWorkerPath(path)

    fun requireAllowedPath(path: String) {
        if (!allowsPath(path)) {
            throw SecurityException("Worker path is outside the safe Kotlin source/test allowlist or is protected")
        }
    }

    fun register(
        registry: VyomFactorySystemRegistry,
        proposed: VyomFactorySystemRegistry.Binding,
        ownerConfirmed: Boolean
    ): VyomFactorySystemRegistry.Binding {
        check(ownerConfirmed) { "Owner confirmation is required to bind the Factory code worker" }
        require(proposed.systemId == SYSTEM_ID)
        require(proposed.allowedPaths == ALLOWED_PATHS)
        createBinding(proposed.repositoryUrl, proposed.commitSha, proposed.registeredAtEpochMs)
        val existing = registry.get(SYSTEM_ID)
        if (existing != null) {
            if (existing.repositoryUrl == proposed.repositoryUrl &&
                existing.commitSha == proposed.commitSha &&
                existing.allowedPaths == proposed.allowedPaths
            ) return existing
            throw SecurityException("Worker already has a different immutable binding; explicit migration is required")
        }
        return registry.register(proposed, ownerConfirmed = true)
    }

    fun migrate(
        registry: VyomFactorySystemRegistry,
        expectedExisting: VyomFactorySystemRegistry.Binding,
        proposed: VyomFactorySystemRegistry.Binding,
        ownerConfirmed: Boolean
    ): VyomFactorySystemRegistry.Binding {
        check(ownerConfirmed) { "Owner confirmation is required to migrate the Factory worker binding" }
        require(expectedExisting.systemId == SYSTEM_ID && proposed.systemId == SYSTEM_ID)
        require(proposed.allowedPaths == ALLOWED_PATHS)
        createBinding(proposed.repositoryUrl, proposed.commitSha, proposed.registeredAtEpochMs)
        return registry.migrate(proposed, expectedExisting, ownerConfirmed = true)
    }
}
