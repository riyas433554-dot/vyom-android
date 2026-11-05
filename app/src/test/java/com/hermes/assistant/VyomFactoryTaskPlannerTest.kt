package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VyomFactoryTaskPlannerTest {
    @Test fun discoversOnlyCapabilitiesInTheClassifiedDomain() {
        val planner = VyomFactoryTaskPlanner()
        val registry = VyomFactoryCapabilityRegistry(
            listOf(
                capability("android_control.set_media_volume", VyomFactoryCapabilityRegistry.Domain.ANDROID_CONTROL, "set_media_volume"),
                capability("repository.inspect", VyomFactoryCapabilityRegistry.Domain.REPOSITORY, "inspect")
            )
        )

        val result = planner.plan(task("Set media volume to 35 percent"), registry)

        assertEquals(
            VyomFactoryTaskPlanner.Plan.CapabilityCandidates(
                VyomFactoryCapabilityRegistry.Domain.ANDROID_CONTROL,
                listOf("android_control.set_media_volume")
            ),
            result
        )
    }

    @Test fun reportsCapabilityGapInsteadOfInventingSpecialist() {
        val result = VyomFactoryTaskPlanner().plan(
            task("Gradle compile failure in repository"),
            VyomFactoryCapabilityRegistry(emptyList())
        )

        assertEquals(
            VyomFactoryTaskPlanner.Plan.BlockedCapability(
                VyomFactoryCapabilityRegistry.Domain.REPOSITORY,
                "no_registered_capability"
            ),
            result
        )
    }

    @Test fun unavailableSpecialistReturnsItsDiagnosticReason() {
        val registry = VyomFactoryCapabilityRegistry(
            listOf(
                capability(
                    "memory.retrieve",
                    VyomFactoryCapabilityRegistry.Domain.MEMORY,
                    "retrieve",
                    available = false,
                    unavailableReason = "memory provider not connected"
                )
            )
        )

        val result = VyomFactoryTaskPlanner().plan(task("Remember this memory fact"), registry)

        assertEquals(
            VyomFactoryTaskPlanner.Plan.BlockedCapability(
                VyomFactoryCapabilityRegistry.Domain.MEMORY,
                "memory provider not connected"
            ),
            result
        )
    }

    @Test fun unknownTaskRequiresClarificationAndDoesNotSelectCapability() {
        val registry = VyomFactoryCapabilityRegistry(
            listOf(capability("repository.inspect", VyomFactoryCapabilityRegistry.Domain.REPOSITORY, "inspect"))
        )

        val result = VyomFactoryTaskPlanner().plan(task("Vyom misunderstood my request"), registry)

        assertEquals(VyomFactoryTaskPlanner.Plan.NeedsOwnerClarification("no_domain_signal"), result)
    }

    private fun capability(
        id: String,
        domain: VyomFactoryCapabilityRegistry.Domain,
        operation: String,
        available: Boolean = true,
        unavailableReason: String? = null
    ) = VyomFactoryCapabilityRegistry.Capability(
        id = id,
        version = 1,
        domain = domain,
        operations = setOf(operation),
        risk = VyomFactoryCapabilityRegistry.Risk.R0,
        requiresOwnerApproval = false,
        mutating = false,
        reversible = false,
        rollbackCapabilityId = null,
        idempotent = true,
        available = available,
        unavailableReason = unavailableReason
    )

    private fun task(title: String) = VyomFactoryGitHubClient.Task(
        number = 8,
        title = title,
        body = "Expected outcome is documented",
        state = "open",
        url = "https://github.com/example/repo/issues/8",
        updatedAt = "2026-11-05T12:00:00Z",
        metadata = VyomFactoryStatus.Metadata(
            priority = "P1",
            status = "open",
            source = "factory",
            sourceId = "planner-test",
            idempotencyKey = "factory:planner-test",
            events = emptyList()
        )
    )
}
