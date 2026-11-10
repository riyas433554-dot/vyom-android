package com.hermes.assistant

/**
 * Typed discovery index for capabilities owned by Universal Factory specialists.
 *
 * This registry describes what may be selected; it does not execute capabilities.
 * Execution remains behind the existing fenced runner and registered adapters.
 */
internal class VyomFactoryCapabilityRegistry(
    capabilities: Collection<Capability>
) {
    enum class Domain {
        COMPLAINT,
        ERROR_DIAGNOSTICS,
        REPOSITORY,
        DESIGN_UI,
        ANDROID_CONTROL,
        MEMORY,
        SKILLS
    }

    enum class Risk { R0, R1, R2, R3 }

    data class Capability(
        val id: String,
        val version: Int,
        val domain: Domain,
        val operations: Set<String>,
        val risk: Risk,
        val requiresOwnerApproval: Boolean,
        val mutating: Boolean,
        val reversible: Boolean,
        val rollbackCapabilityId: String?,
        val idempotent: Boolean,
        val available: Boolean,
        val unavailableReason: String?
    ) {
        init {
            require(ID_PATTERN.matches(id)) { "Invalid capability ID" }
            require(version > 0) { "Capability version must be positive" }
            require(operations.isNotEmpty() && operations.all(OPERATION_PATTERN::matches)) {
                "Capability must declare valid operations"
            }
            require(!mutating || risk < Risk.R2 || requiresOwnerApproval) {
                "R2/R3 mutations require owner approval"
            }
            require(!reversible || !rollbackCapabilityId.isNullOrBlank()) {
                "Reversible capability requires a rollback handler"
            }
            require(available || !unavailableReason.isNullOrBlank()) {
                "Unavailable capability requires a diagnostic reason"
            }
            require(available || unavailableReason == null || unavailableReason.length <= MAX_REASON_LENGTH) {
                "Capability unavailable reason is too long"
            }
        }

        companion object {
            private val ID_PATTERN = Regex("[a-z][a-z0-9_.-]{1,95}")
            private val OPERATION_PATTERN = Regex("[a-z][a-z0-9_]{1,63}")
            private const val MAX_REASON_LENGTH = 240
        }
    }

    private val byId: Map<String, Capability>

    init {
        val indexed = LinkedHashMap<String, Capability>()
        capabilities.forEach { capability ->
            require(!indexed.containsKey(capability.id)) {
                "Duplicate capability ID: ${capability.id}"
            }
            indexed[capability.id] = capability
        }
        byId = indexed.toMap()
    }

    fun resolve(id: String): Capability? = byId[id]

    fun unavailableReason(id: String): String? =
        byId[id]?.takeUnless { it.available }?.unavailableReason

    fun discover(domain: Domain, operation: String): List<Capability> {
        if (!operation.matches(Regex("[a-z][a-z0-9_]{1,63}"))) return emptyList()
        return byId.values
            .asSequence()
            .filter { it.available && it.domain == domain && operation in it.operations }
            .sortedBy { it.id }
            .toList()
    }

    fun all(): List<Capability> = byId.values.sortedBy { it.id }
}
