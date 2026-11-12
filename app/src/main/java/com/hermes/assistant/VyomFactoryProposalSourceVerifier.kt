package com.hermes.assistant

/** Captures and rechecks every proposal source file against its immutable registry-bound commit. */
internal object VyomFactoryProposalSourceVerifier {
    fun capture(
        plan: VyomFactoryRepositoryChangePolicy.Plan,
        binding: VyomFactorySystemRegistry.Binding,
        registry: VyomFactorySystemRegistry,
        readBoundFile: (String, String, VyomFactorySystemRegistry) -> VyomFactoryRepositoryClient.FilePreview
    ): Map<String, String> {
        require(plan.systemId == binding.systemId && plan.baseCommitSha == binding.commitSha) {
            "Proposal source verification is not pinned to the registered commit"
        }
        return plan.changes.associate { change ->
            val file = readBoundFile(plan.systemId, change.path, registry)
            require(file.path == change.path && file.blobSha.isNotBlank() &&
                file.contentSha256.matches(SHA256)) {
                "GitHub source file failed immutable-base SHA-256 verification: ${change.path}"
            }
            change.path to file.contentSha256
        }.also { hashes ->
            require(hashes.keys == plan.changes.map { it.path }.toSet()) {
                "Not every proposal file has a verified immutable-base SHA-256"
            }
        }
    }

    fun verify(
        candidate: VyomFactoryRepositoryChangeReviewDialog.Candidate,
        registry: VyomFactorySystemRegistry,
        readBoundFile: (String, String, VyomFactorySystemRegistry) -> VyomFactoryRepositoryClient.FilePreview
    ) {
        val plan = candidate.plan
        require(candidate.sourceFileSha256.keys == plan.changes.map { it.path }.toSet() &&
            candidate.sourceFileSha256.values.all { it.matches(SHA256) }) {
            throw SecurityException("Proposal is missing exact-base source SHA-256 evidence")
        }
        require(registry.get(candidate.binding.systemId) == candidate.binding &&
            plan.systemId == candidate.binding.systemId &&
            plan.baseCommitSha == candidate.binding.commitSha) {
            throw SecurityException("Proposal source binding changed before review")
        }
        plan.changes.forEach { change ->
            val file = readBoundFile(plan.systemId, change.path, registry)
            val expected = candidate.sourceFileSha256[change.path]
            if (file.path != change.path || file.contentSha256 != expected) {
                throw SecurityException("Proposal source SHA-256 mismatch at bound commit: ${change.path}")
            }
        }
    }

    private val SHA256 = Regex("^[a-f0-9]{64}$")
}