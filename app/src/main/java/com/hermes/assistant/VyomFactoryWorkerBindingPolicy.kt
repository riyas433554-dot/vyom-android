package com.hermes.assistant

/** Narrow owner-approved binding for the isolated real-worker test lane. */
internal object VyomFactoryWorkerBindingPolicy {
    const val SYSTEM_ID = "worker"
    const val ALLOWED_PATH = "README.md"

    fun createBinding(repositoryUrl: String, baseSha: String, registeredAtEpochMs: Long): VyomFactorySystemRegistry.Binding {
        require(repositoryUrl == repositoryUrl.trim()) { "Repository URL must not contain surrounding whitespace" }
        val source = VyomFactoryRepositoryIntake.parseUrl(repositoryUrl)
            ?: throw IllegalArgumentException("Enter a supported HTTPS GitHub owner/repository URL")
        require(source.canonicalUrl == repositoryUrl) {
            "Repository URL must be canonical (https://github.com/owner/repository)"
        }
        require(baseSha.matches(Regex("^[a-f0-9]{40}$"))) {
            "Base SHA must be the exact 40-character lowercase hexadecimal commit"
        }
        require(registeredAtEpochMs > 0L)
        return VyomFactorySystemRegistry.Binding(
            systemId = SYSTEM_ID,
            repositoryUrl = source.canonicalUrl,
            commitSha = baseSha,
            allowedPaths = listOf(ALLOWED_PATH),
            registeredAtEpochMs = registeredAtEpochMs
        )
    }

    fun allowsPath(path: String): Boolean = path == ALLOWED_PATH

    fun requireAllowedPath(path: String) {
        if (!allowsPath(path)) throw SecurityException("Worker test binding permits README.md only")
    }

    fun register(
        registry: VyomFactorySystemRegistry,
        proposed: VyomFactorySystemRegistry.Binding,
        ownerConfirmed: Boolean
    ): VyomFactorySystemRegistry.Binding {
        check(ownerConfirmed) { "Owner confirmation is required to bind the test worker" }
        require(proposed.systemId == SYSTEM_ID)
        require(proposed.allowedPaths == listOf(ALLOWED_PATH))
        // Reuse shared URL/SHA validation before writing to the existing system registry.
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
}
