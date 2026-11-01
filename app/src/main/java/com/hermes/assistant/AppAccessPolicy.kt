package com.hermes.assistant

import android.content.Context

/** User-managed per-app gate for Accessibility UI control. */
object AppAccessPolicy {
    data class Snapshot(val masterEnabled: Boolean, val enabledPackages: Set<String>)
    fun isAllowed(snapshot: Snapshot, packageName: String): Boolean =
        snapshot.masterEnabled && packageName.isNotBlank() && packageName in snapshot.enabledPackages
    fun snapshot(context: Context): Snapshot {
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        return Snapshot(prefs.getBoolean(KEY_MASTER,true),prefs.getStringSet(KEY_PACKAGES,emptySet())?.toSet().orEmpty())
    }
    fun initializeForInstalledApps(context: Context, packageNames: Collection<String>) {
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        if(prefs.getBoolean(KEY_INITIALIZED,false)) return
        prefs.edit().putBoolean(KEY_MASTER,true)
            .putStringSet(KEY_PACKAGES,packageNames.filter(String::isNotBlank).toSet())
            .putBoolean(KEY_INITIALIZED,true).apply()
    }
    fun setMasterEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(KEY_MASTER,enabled).apply()
    }
    fun setPackageEnabled(context: Context, packageName: String, enabled: Boolean) {
        if(packageName.isBlank()) return
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val packages=prefs.getStringSet(KEY_PACKAGES,emptySet())?.toMutableSet() ?: mutableSetOf()
        if(enabled) packages.add(packageName) else packages.remove(packageName)
        prefs.edit().putStringSet(KEY_PACKAGES,packages).putBoolean(KEY_INITIALIZED,true).apply()
    }
    const val PREFS="vyom_app_access"
    private const val KEY_MASTER="master_enabled"
    private const val KEY_PACKAGES="enabled_packages"
    private const val KEY_INITIALIZED="initialized"
}