package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryCapabilityRegistryTest {
    @Test fun discoversOnlyAvailableCapabilitiesMatchingDomainAndOperation() {
        val registry = VyomFactoryCapabilityRegistry(
            listOf(
                capability("repo.inspect", VyomFactoryCapabilityRegistry.Domain.REPOSITORY, "inspect", available = true),
                capability("repo.patch", VyomFactoryCapabilityRegistry.Domain.REPOSITORY, "patch", available = false, unavailableReason = "approval missing"),
                capability("memory.read", VyomFactoryCapabilityRegistry.Domain.MEMORY, "read")
            )
        )

        assertEquals(listOf("repo.inspect"), registry.discover(
            VyomFactoryCapabilityRegistry.Domain.REPOSITORY, "inspect"
        ).map { it.id })
        assertTrue(registry.discover(VyomFactoryCapabilityRegistry.Domain.REPOSITORY, "patch").isEmpty())
        assertEquals(listOf("memory.read"), registry.discover(
            VyomFactoryCapabilityRegistry.Domain.MEMORY, "read"
        ).map { it.id })
    }

    @Test fun unavailableCapabilityExplainsWhyItCannotRun() {
        val item = capability(
            "repo.write",
            VyomFactoryCapabilityRegistry.Domain.REPOSITORY,
            "write",
            available = false,
            unavailableReason = "owner approval required"
        )
        val registry = VyomFactoryCapabilityRegistry(listOf(item))

        assertEquals("owner approval required", registry.unavailableReason("repo.write"))
        assertTrue(registry.discover(VyomFactoryCapabilityRegistry.Domain.REPOSITORY, "write").isEmpty())
    }

    @Test fun rejectsDuplicateCapabilityIds() {
        val first = capability("repo.inspect", VyomFactoryCapabilityRegistry.Domain.REPOSITORY, "inspect")
        val duplicate = first.copy(version = 2)

        val error = runCatching {
            VyomFactoryCapabilityRegistry(listOf(first, duplicate))
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test fun rejectsMutatingHighRiskCapabilityWithoutOwnerApproval() {
        val error = runCatching {
            capability(
                "security.signing",
                VyomFactoryCapabilityRegistry.Domain.REPOSITORY,
                "sign",
                risk = VyomFactoryCapabilityRegistry.Risk.R3,
                requiresApproval = false,
                mutating = true
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test fun rejectsReversibleCapabilityWithoutRollbackHandler() {
        val error = runCatching {
            capability(
                "repo.patch",
                VyomFactoryCapabilityRegistry.Domain.REPOSITORY,
                "patch",
                reversible = true,
                rollbackCapabilityId = null
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test fun unknownCapabilityDoesNotResolveToFallback() {
        val registry = VyomFactoryCapabilityRegistry(emptyList())

        assertNull(registry.resolve("unknown"))
        assertTrue(registry.discover(VyomFactoryCapabilityRegistry.Domain.DESIGN_UI, "implement").isEmpty())
    }

    private fun capability(
        id: String,
        domain: VyomFactoryCapabilityRegistry.Domain,
        operation: String,
        available: Boolean = true,
        unavailableReason: String? = null,
        risk: VyomFactoryCapabilityRegistry.Risk = VyomFactoryCapabilityRegistry.Risk.R0,
        requiresApproval: Boolean = false,
        mutating: Boolean = false,
        reversible: Boolean = false,
        rollbackCapabilityId: String? = null
    ) = VyomFactoryCapabilityRegistry.Capability(
        id = id,
        version = 1,
        domain = domain,
        operations = setOf(operation),
        risk = risk,
        requiresOwnerApproval = requiresApproval,
        mutating = mutating,
        reversible = reversible,
        rollbackCapabilityId = rollbackCapabilityId,
        idempotent = !mutating,
        available = available,
        unavailableReason = unavailableReason
    )
}
