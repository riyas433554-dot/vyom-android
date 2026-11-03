package com.hermes.assistant

/** Pure guard/merge policy for the explicitly owner-requested Accessibility reconnect. */
internal object AccessibilityReconnectPolicy {
    private val packagePattern = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$")
    private val classPattern = Regex("^[A-Za-z][A-Za-z0-9_.$]*$")

    fun canReconnect(writeSecureSettingsGranted: Boolean, component: String): Boolean =
        writeSecureSettingsGranted && isValidComponent(component)

    fun mergeEnabledServices(current: String?, component: String): String? {
        if (!isValidComponent(component)) return null
        val existing = current.orEmpty().split(':').map(String::trim).filter(String::isNotEmpty)
        val preserved = existing.filterNot { it.equals(component, ignoreCase = true) }
        return (preserved + component).joinToString(":")
    }

    fun isEnabled(current: String?, component: String): Boolean =
        isValidComponent(component) && current.orEmpty().split(':')
            .any { it.trim().equals(component, ignoreCase = true) }

    /** Android Accessibility is effective only when the global switch and Vyom service are both enabled. */
    fun isEffectivelyEnabled(globalEnabled: Boolean, enabledServices: String?, component: String): Boolean =
        globalEnabled && isEnabled(enabledServices, component)

    private fun isValidComponent(component: String): Boolean {
        val parts = component.split('/')
        return parts.size == 2 &&
            parts[0].matches(packagePattern) &&
            parts[1].matches(classPattern)
    }
}
