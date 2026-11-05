package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VyomFactoryTaskTriageTest {
    @Test fun triagesActiveTasksInCanonicalPriorityAndFifoOrder() {
        val tasks = listOf(
            task(3, "Set media volume to 35 percent", "P2", "2026-11-03T00:00:00Z"),
            task(2, "Unknown complaint", "P1", "2026-11-02T00:00:00Z"),
            task(1, "Gradle compile failure", "P1", "2026-11-01T00:00:00Z"),
            task(4, "Paused memory request", "P0", "2026-10-31T00:00:00Z", status = "paused")
        )
        val capabilities = VyomFactoryCapabilityRegistry(
            listOf(
                VyomFactoryCapabilityRegistry.Capability(
                    id = "android_control.set_media_volume",
                    version = 1,
                    domain = VyomFactoryCapabilityRegistry.Domain.ANDROID_CONTROL,
                    operations = setOf("set_media_volume"),
                    risk = VyomFactoryCapabilityRegistry.Risk.R1,
                    requiresOwnerApproval = false,
                    mutating = true,
                    reversible = false,
                    rollbackCapabilityId = null,
                    idempotent = true,
                    available = true,
                    unavailableReason = null
                )
            )
        )

        val decisions = VyomFactoryTaskTriage().evaluate(tasks, capabilities)

        assertEquals(listOf(1, 2, 3), decisions.map { it.task.number })
        assertEquals(
            VyomFactoryTaskPlanner.Plan.BlockedCapability(
                VyomFactoryCapabilityRegistry.Domain.REPOSITORY,
                "no_registered_capability"
            ),
            decisions[0].plan
        )
        assertEquals(
            VyomFactoryTaskPlanner.Plan.NeedsOwnerClarification("no_domain_signal"),
            decisions[1].plan
        )
        assertEquals(
            VyomFactoryTaskPlanner.Plan.CapabilityCandidates(
                VyomFactoryCapabilityRegistry.Domain.ANDROID_CONTROL,
                listOf("android_control.set_media_volume")
            ),
            decisions[2].plan
        )
    }

    @Test fun candidateDiscoveryIsExplicitlyNotExecutionReadiness() {
        val decision = VyomFactoryTaskTriage().evaluate(
            listOf(task(9, "Set media volume to 35 percent", "P1", "2026-11-01T00:00:00Z")),
            registryWithVolumeCapability()
        ).single()

        assertEquals(false, decision.executionReady)
        assertEquals(
            "triage:v1:capability_candidates:ANDROID_CONTROL:android_control.set_media_volume",
            decision.eventKey
        )
    }

    @Test fun triageEventKeysAreStableForDurableDeduplication() {
        val triage = VyomFactoryTaskTriage()
        val registry = VyomFactoryCapabilityRegistry(emptyList())
        val task = task(10, "Unknown complaint", "P2", "2026-11-01T00:00:00Z")

        val first = triage.evaluate(listOf(task), registry).single()
        val second = triage.evaluate(listOf(task), registry).single()

        assertEquals(first.eventKey, second.eventKey)
    }

    private fun registryWithVolumeCapability() = VyomFactoryCapabilityRegistry(
        listOf(
            VyomFactoryCapabilityRegistry.Capability(
                id = "android_control.set_media_volume",
                version = 1,
                domain = VyomFactoryCapabilityRegistry.Domain.ANDROID_CONTROL,
                operations = setOf("set_media_volume"),
                risk = VyomFactoryCapabilityRegistry.Risk.R1,
                requiresOwnerApproval = false,
                mutating = true,
                reversible = false,
                rollbackCapabilityId = null,
                idempotent = true,
                available = true,
                unavailableReason = null
            )
        )
    )

    private fun task(
        number: Int,
        title: String,
        priority: String,
        createdAt: String,
        status: String = "open"
    ) = VyomFactoryGitHubClient.Task(
        number = number,
        title = title,
        body = "Expected outcome is documented",
        state = "open",
        url = "https://github.com/example/repo/issues/$number",
        updatedAt = createdAt,
        metadata = VyomFactoryStatus.Metadata(
            priority = priority,
            status = status,
            source = "factory",
            sourceId = "triage-$number",
            idempotencyKey = "factory:triage-$number",
            events = emptyList()
        ),
        createdAt = createdAt
    )
}
