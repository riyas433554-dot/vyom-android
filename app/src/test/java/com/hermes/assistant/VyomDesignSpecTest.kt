package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomDesignSpecTest {
    @Test fun designTokensMatchTheSuppliedHudReference() {
        assertEquals(0xFF070B14.toInt(), VyomDesignSpec.BACKGROUND)
        assertEquals(0xFF0F1A2E.toInt(), VyomDesignSpec.GLASS)
        assertEquals(0xFF22D3EE.toInt(), VyomDesignSpec.CYAN)
        assertEquals(0xFF8B5CF6.toInt(), VyomDesignSpec.VIOLET)
        assertEquals(0xFFFBBF24.toInt(), VyomDesignSpec.AMBER)
        assertEquals(0xFF34D399.toInt(), VyomDesignSpec.GREEN)
        assertEquals(0xFFF43F5E.toInt(), VyomDesignSpec.ROSE)
        assertEquals(0xFFE6F1FF.toInt(), VyomDesignSpec.TEXT_PRIMARY)
        assertEquals(0xFF8FA3BF.toInt(), VyomDesignSpec.TEXT_SECONDARY)
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
            "tasks", "control", "assistant_control", "skills", "connections", "memory",
            "instructions", "voice", "system", "icons", "settings"
        ).forEach { assertTrue("Missing drawer destination: $it", ids.contains(it)) }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test fun orbStatesHaveExplicitVisualSemantics() {
        assertEquals(VyomDesignSpec.CYAN, VyomDesignSpec.orbAccent(VyomDesignSpec.OrbState.READY))
        assertEquals(VyomDesignSpec.CYAN, VyomDesignSpec.orbAccent(VyomDesignSpec.OrbState.LISTENING))
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

}
