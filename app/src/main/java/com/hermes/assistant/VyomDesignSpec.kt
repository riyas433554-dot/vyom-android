package com.hermes.assistant

/**
 * Single source of truth for the approved Vyom HUD navigation and visual states.
 * Screen implementations may be staged; a destination marked enabled=false is
 * deliberately visible as planned, never represented as a working feature.
 */
internal object VyomDesignSpec {
    const val BACKGROUND = 0xFF070B14.toInt()
    const val GLASS = 0xFF0F1A2E.toInt()
    const val CYAN = 0xFF22D3EE.toInt()
    const val VIOLET = 0xFF8B5CF6.toInt()
    const val AMBER = 0xFFFBBF24.toInt()
    const val GREEN = 0xFF34D399.toInt()
    const val ROSE = 0xFFF43F5E.toInt()
    const val TEXT_PRIMARY = 0xFFE6F1FF.toInt()
    const val TEXT_SECONDARY = 0xFF8FA3BF.toInt()

    data class NavItem(
        val id: String,
        val label: String,
        val section: String = "",
        val enabled: Boolean = true
    )

    val bottomNavigation = listOf(
        NavItem("home", "Home"),
        NavItem("chat", "Chat"),
        NavItem("live", "Live"),
        NavItem("tasks", "Tasks"),
        NavItem("profile", "Profile")
    )

    data class SkillItem(
        val id: String,
        val label: String,
        val description: String,
        val risk: String = "SAFE"
    )

    val deviceSkills = listOf(
        SkillItem("open_app", "Open apps", "Launch an installed app from the Android launcher."),
        SkillItem("volume", "Media volume", "Raise, lower, mute or set media volume."),
        SkillItem("torch", "Torch", "Turn the device flashlight on or off."),
        SkillItem("settings", "System settings panels", "Open Android settings screens; changes remain user-controlled.")
    )

    val drawerItems = listOf(
        NavItem("home", "Home", "Workspace"),
        NavItem("automations", "Automations", "Workspace"),
        NavItem("animations", "Animations", "Workspace", false),
        NavItem("videos", "Videos", "Workspace", false),
        NavItem("images", "Images", "Workspace", false),
        NavItem("vfx", "VFX", "Workspace", false),
        NavItem("cgi", "CGI", "Workspace", false),
        NavItem("sfx", "SFX", "Workspace", false),
        NavItem("tasks", "Tasks", "Assistant"),
        NavItem("control", "Control", "Assistant"),
        NavItem("assistant_control", "Assistant Control", "Assistant"),
        NavItem("skills", "Skills", "Assistant"),
        NavItem("connections", "Connections", "Assistant"),
        NavItem("memory", "Memory", "Brain"),
        NavItem("instructions", "Instructions", "Brain", false),
        NavItem("voice", "Voice", "Brain"),
        NavItem("system", "System", "System"),
        NavItem("icons", "Icons", "System", false),
        NavItem("settings", "Settings", "System")
    )

    enum class OrbState {
        READY, LISTENING, HEARING, THINKING, SPEAKING, MUTED,
        TASK_RUNNING, CONTROL_ACTIVE, CONFIRM_NEEDED, ERROR, OFFLINE
    }

    fun orbAccent(state: OrbState): Int = when (state) {
        OrbState.READY, OrbState.LISTENING, OrbState.HEARING -> CYAN
        OrbState.THINKING, OrbState.SPEAKING -> VIOLET
        OrbState.TASK_RUNNING, OrbState.CONTROL_ACTIVE, OrbState.CONFIRM_NEEDED -> AMBER
        OrbState.MUTED, OrbState.OFFLINE -> TEXT_SECONDARY
        OrbState.ERROR -> ROSE
    }

    /** Renderer modes: cyan=0, amber=1, violet=2, green=3, muted/offline=4, error=5. */
    fun orbMode(state: OrbState): Int = when (state) {
        OrbState.READY, OrbState.LISTENING, OrbState.HEARING -> 0
        OrbState.TASK_RUNNING, OrbState.CONTROL_ACTIVE, OrbState.CONFIRM_NEEDED -> 1
        OrbState.THINKING, OrbState.SPEAKING -> 2
        OrbState.MUTED, OrbState.OFFLINE -> 4
        OrbState.ERROR -> 5
    }
}
