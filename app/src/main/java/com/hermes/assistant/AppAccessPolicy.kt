package com.hermes.assistant

import android.content.Context

/** User-controlled allowlist for accessibility actions. New packages default to OFF. */
internal object AppAccessPolicy {
    data class Snapshot(val masterEnabled: Boolean, val enabledPackages: Set<String>)
    private const val PREFS = "vyom_app_access"
    private const val MASTER = "master_enabled"
    private const val INITIALIZED = "initialized"
    private const val PACKAGES = "enabled_packages"

    private val blockedMarkers = listOf(
        "bank", "banking", "upi", "wallet", "payment", "paytm", "phonepe", "googlepay",
        "google pay", "gpay", "paypal", "amazonpay", "amazon pay", "mobikwik", "freecharge", "bharatpe",
        "cred", "zerodha", "groww", "angelone", "trading", "broker", "investment", "finance",
        "sbi", "hdfc", "icici", "axis", "kotak", "idfc", "canara", "pnb", "indusind", "yesbank"
    )

    internal fun isBlockedIdentifier(packageName: String, label: String = ""): Boolean {
        val identity = (packageName + " " + label).lowercase()
        return blockedMarkers.any { it in identity }
    }

    fun isBlocked(context: Context, packageName: String): Boolean {
        if (isBlockedIdentifier(packageName)) return true
        val label = try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) { "" }
        return isBlockedIdentifier(packageName, label)
    }

    internal fun initialSnapshot(): Snapshot = Snapshot(masterEnabled = false, enabledPackages = emptySet())

    fun initializeForInstalledApps(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(INITIALIZED, false)) {
            val initial = initialSnapshot()
            prefs.edit().putBoolean(MASTER, initial.masterEnabled)
                .putStringSet(PACKAGES, initial.enabledPackages)
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
        if (packageName.isBlank() || isBlocked(context, packageName)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val packages = prefs.getStringSet(PACKAGES, mutableSetOf()).orEmpty().toMutableSet()
        if (enabled) packages.add(packageName) else packages.remove(packageName)
        prefs.edit().putStringSet(PACKAGES, packages).apply()
    }

    fun isAllowed(context: Context, packageName: String): Boolean {
        if (isBlocked(context, packageName)) return false
        val state = snapshot(context)
        return isAllowed(state.masterEnabled, state.enabledPackages, packageName, context.packageName)
    }

    internal fun isAllowed(
        masterEnabled: Boolean,
        enabledPackages: Set<String>,
        packageName: String,
        controllerPackage: String
    ): Boolean = masterEnabled && packageName.isNotBlank() &&
        (packageName == controllerPackage || packageName in enabledPackages)

    internal fun isAllowed(masterEnabled: Boolean, enabledPackages: Set<String>, packageName: String): Boolean =
        isAllowed(masterEnabled, enabledPackages, packageName, "")
}
