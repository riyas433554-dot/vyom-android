package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class VyomCapabilityCatalogTest {
    @Test
    fun catalogContainsAllThirtyTwoRequestedCapabilitiesWithoutDuplicates() {
        val entries = VyomCapabilityCatalog.entries
        assertEquals(32, entries.size)
        assertEquals(32, entries.map { it.id }.distinct().size)
        assertTrue(entries.all { it.name.isNotBlank() && it.category.isNotBlank() && it.summary.isNotBlank() })
    }

    @Test
    fun onlyExistingGitHubMcpIntegrationIsMarkedIntegrated() {
        val integrated = VyomCapabilityCatalog.entries.filter {
            it.status == VyomCapabilityCatalog.Status.INTEGRATED
        }
        assertEquals(listOf("github-mcp"), integrated.map { it.id })
        assertFalse(VyomCapabilityCatalog.entries.any {
            it.id != "github-mcp" && it.status == VyomCapabilityCatalog.Status.INTEGRATED
        })
    }

    @Test
    fun memoryProfileClearlySeparatesAvailableAndPlannedPowers() {
        val facts = VyomCapabilityCatalog.memoryFacts()
        assertTrue(facts.any { it.contains("GitHub MCP") && it.contains("integrated") })
        assertTrue(facts.any { it.contains("not installed") })
        assertTrue(facts.any { it.contains("Gustavo") && it.contains("unresolved") })
    }
}
