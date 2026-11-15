package com.hermes.assistant

/** Owner-controlled gate for loading local worker skills into AI context. */
internal object VyomFactorySkillsEnabledPolicy {
    const val PREFERENCES_NAME = "vyom_factory_worker_context"
    const val PREFERENCE_KEY = "skills_enabled"
    const val DEFAULT_ENABLED = false

    /** Missing preference is OFF; only an explicit owner-saved true enables skill loading. */
    fun isEnabled(storedValue: Boolean?): Boolean = storedValue == true
}
