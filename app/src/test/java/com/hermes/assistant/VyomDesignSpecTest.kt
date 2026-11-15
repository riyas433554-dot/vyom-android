package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomDesignSpecTest {
    @Test fun designTokensMatchTheApprovedObsidianPlatinumPalette() {
        assertEquals(0xFF090B10.toInt(), VyomDesignSpec.BACKGROUND)
        assertEquals(0xFF171A22.toInt(), VyomDesignSpec.GLASS)
        assertEquals(0xFF7775E8.toInt(), VyomDesignSpec.INDIGO)
        assertEquals(0xFF9997F0.toInt(), VyomDesignSpec.VIOLET)
        assertEquals(0xFFC6AD82.toInt(), VyomDesignSpec.AMBER)
        assertEquals(0xFF55B995.toInt(), VyomDesignSpec.GREEN)
        assertEquals(0xFFD96C7A.toInt(), VyomDesignSpec.ROSE)
        assertEquals(0xFFE8E8ED.toInt(), VyomDesignSpec.TEXT_PRIMARY)
        assertEquals(0xFF9296A3.toInt(), VyomDesignSpec.TEXT_SECONDARY)
    }

    @Test fun bottomNavigationMatchesFivePrimaryDestinations() {
        assertEquals(
            listOf("home", "chat", "live", "tasks", "profile"),
            VyomDesignSpec.bottomNavigation.map { it.id }
        )
    }

    @Test fun drawerExposesExistingAndDesignedWorkspaceDestinations() {
        val ids = VyomDesignSpec.drawerItems.map { it.id }
        listOf(
            "home", "automations", "animations", "videos", "images", "vfx", "cgi", "sfx",
            "tasks", "control", "assistant_control", "skills", "connections", "audit", "permissions", "memory",
            "instructions", "voice", "system", "icons", "settings"
        ).forEach { assertTrue("Missing drawer destination: $it", ids.contains(it)) }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test fun orbStatesHaveExplicitVisualSemantics() {
        assertEquals(VyomDesignSpec.INDIGO, VyomDesignSpec.orbAccent(VyomDesignSpec.OrbState.READY))
        assertEquals(VyomDesignSpec.INDIGO, VyomDesignSpec.orbAccent(VyomDesignSpec.OrbState.LISTENING))
        assertEquals(VyomDesignSpec.VIOLET, VyomDesignSpec.orbAccent(VyomDesignSpec.OrbState.THINKING))
        assertEquals(VyomDesignSpec.VIOLET, VyomDesignSpec.orbAccent(VyomDesignSpec.OrbState.SPEAKING))
        assertEquals(VyomDesignSpec.AMBER, VyomDesignSpec.orbAccent(VyomDesignSpec.OrbState.TASK_RUNNING))
        assertEquals(VyomDesignSpec.AMBER, VyomDesignSpec.orbAccent(VyomDesignSpec.OrbState.CONTROL_ACTIVE))
        assertEquals(VyomDesignSpec.ROSE, VyomDesignSpec.orbAccent(VyomDesignSpec.OrbState.ERROR))
        assertEquals(VyomDesignSpec.TEXT_SECONDARY, VyomDesignSpec.orbAccent(VyomDesignSpec.OrbState.OFFLINE))
    }

    @Test fun orbStatesMapToRendererModesWithoutChangingThemePreferences() {
        assertEquals(0, VyomDesignSpec.orbMode(VyomDesignSpec.OrbState.READY))
        assertEquals(0, VyomDesignSpec.orbMode(VyomDesignSpec.OrbState.LISTENING))
        assertEquals(2, VyomDesignSpec.orbMode(VyomDesignSpec.OrbState.THINKING))
        assertEquals(2, VyomDesignSpec.orbMode(VyomDesignSpec.OrbState.SPEAKING))
        assertEquals(1, VyomDesignSpec.orbMode(VyomDesignSpec.OrbState.TASK_RUNNING))
        assertEquals(1, VyomDesignSpec.orbMode(VyomDesignSpec.OrbState.CONTROL_ACTIVE))
        assertEquals(4, VyomDesignSpec.orbMode(VyomDesignSpec.OrbState.MUTED))
        assertEquals(4, VyomDesignSpec.orbMode(VyomDesignSpec.OrbState.OFFLINE))
        assertEquals(5, VyomDesignSpec.orbMode(VyomDesignSpec.OrbState.ERROR))
    }


    @Test fun skillCatalogExposesBoundedUiAutomationAsAConfirmationGatedSkill() {
        assertEquals(
            listOf("open_app", "volume", "torch", "settings", "ui_agent"),
            VyomDesignSpec.deviceSkills.map { it.id }
        )
        assertTrue(VyomDesignSpec.deviceSkills.filter { it.id != "ui_agent" }.all { it.risk == "SAFE" })
        assertEquals("CONFIRM", VyomDesignSpec.deviceSkills.first { it.id == "ui_agent" }.risk)
    }


    @Test fun taskStatusPillsUseDistinctHonestStates() {
        assertEquals(VyomDesignSpec.AMBER, VyomDesignSpec.taskStatusAccent("queued"))
        assertEquals(VyomDesignSpec.AMBER, VyomDesignSpec.taskStatusAccent("pending"))
        assertEquals(VyomDesignSpec.AMBER, VyomDesignSpec.taskStatusAccent("running"))
        assertEquals(VyomDesignSpec.GREEN, VyomDesignSpec.taskStatusAccent("completed"))
        assertEquals(VyomDesignSpec.ROSE, VyomDesignSpec.taskStatusAccent("failed"))
        assertEquals(VyomDesignSpec.TEXT_SECONDARY, VyomDesignSpec.taskStatusAccent("unknown"))
    }


    @Test fun drawerShowsOnlyImplementedDestinationsAndNeverRendersSoonPlaceholders() {
        val visibleIds = VyomDesignSpec.visibleDrawerItems().map { it.id }
        assertTrue(visibleIds.containsAll(listOf("home", "automations", "tasks", "control", "skills", "memory")))
        assertFalse(visibleIds.any { it in setOf("animations", "videos", "images", "vfx", "cgi", "sfx", "icons") })
        assertEquals(visibleIds.size, visibleIds.distinct().size)
    }

    @Test fun drawerSearchOnlyReturnsEnabledDestinations() {
        val results = VyomDesignSpec.visibleDrawerItems("vfx")
        assertTrue(results.isEmpty())
        assertTrue(VyomDesignSpec.visibleDrawerItems("memory").any { it.id == "memory" })
    }

    @Test fun legacyCyanThemePreferenceIsCanonicalizedToIndigoWithoutChangingOtherAccents() {
        val normalizer = VyomTheme::class.java.methods.firstOrNull {
            it.name == "canonicalAccentTheme" &&
                it.parameterTypes.contentEquals(arrayOf(String::class.java))
        }
        assertTrue("Theme preference normalizer must be exposed for migration", normalizer != null)
        assertEquals("indigo", normalizer!!.invoke(VyomTheme, "cyan"))
        assertEquals("indigo", normalizer.invoke(VyomTheme, "INDIGO"))
        assertEquals("amber", normalizer.invoke(VyomTheme, "amber"))
        assertEquals("violet", normalizer.invoke(VyomTheme, "violet"))
        assertEquals("green", normalizer.invoke(VyomTheme, "green"))
        assertEquals("danger", normalizer.invoke(VyomTheme, "red"))
        assertEquals("indigo", normalizer.invoke(VyomTheme, null))
        assertEquals("indigo", normalizer.invoke(VyomTheme, "unknown"))
    }

    @Test fun emergencyStopSurfaceRemainsDistinctFromRoseTextAccent() {
        assertEquals(0xFFAA1923.toInt(), VyomTheme.SURFACE_DANGER)
        assertEquals(0xFFD96C7A.toInt(), VyomTheme.ACCENT_DANGER)
    }

    @Test fun readyOrbitUsesElectricIndigoToken() {
        val indigo = VyomDesignSpec::class.java.declaredFields.firstOrNull { it.name == "INDIGO" }
        assertTrue("Design spec must expose the semantically named INDIGO token", indigo != null)
        assertEquals(0xFF7775E8.toInt(), indigo!!.getInt(null))
        assertEquals(0xFF7775E8.toInt(), VyomDesignSpec.orbAccent(VyomDesignSpec.OrbState.READY))
        assertEquals(0xFF7775E8.toInt(), VyomDesignSpec.orbAccent(VyomDesignSpec.OrbState.LISTENING))
    }

}
