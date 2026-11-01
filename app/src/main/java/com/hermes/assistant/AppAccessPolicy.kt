package com.hermes.assistant

import android.content.Context

/** User-controlled allowlist for accessibility actions. New packages default to OFF. */
internal object AppAccessPolicy {
    data class Snapshot(val masterEnabled: Boolean, val enabledPackages: Set<String>)
    private const val PREFS = "vyom_app_access"
    private const val MASTER = "master_enabled"
    private const val INITIALIZED = "initialized"
    private const val PACKAGES = "enabled_packages"

    fun initializeForInstalledApps(context: Context, launchablePackages: Collection<String>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(INITIALIZED, false)) {
            prefs.edit().putBoolean(MASTER, true)
                .putStringSet(PACKAGES, launchablePackages.filter { it.isNotBlank() }.toMutableSet())
                .putBoolean(INITIALIZED, true).apply()
        }
    }

    fun snapshot(context: Context): Snapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Snapshot(prefs.getBoolean(MASTER, false), prefs.getStringSet(PACKAGES, mutableSetOf()).orEmpty().toSet())
    }

    fun setMasterEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(MASTER, enabled).apply()
    }

    fun setPackageEnabled(context: Context, packageName: String, enabled: Boolean) {
        if (packageName.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val packages = prefs.getStringSet(PACKAGES, emptySet()).orEmpty().toMutableSet()
        if (enabled) packages.add(packageName) else packages.remove(packageName)
        prefs.edit().putStringSet(PACKAGES, packages).apply()
    }

    fun isAllowed(context: Context, packageName: String): Boolean {
        val state = snapshot(context)
        return isAllowed(state.masterEnabled, state.enabledPackages, packageName)
    }

    internal fun isAllowed(masterEnabled: Boolean, enabledPackages: Set<String>, packageName: String): Boolean =
        masterEnabled && packageName.isNotBlank() && packageName in enabledPackages
}
