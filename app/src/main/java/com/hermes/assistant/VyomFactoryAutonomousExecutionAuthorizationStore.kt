package com.hermes.assistant

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

/** Encrypted, device-local owner consent for unattended bounded repository execution. */
internal class VyomFactoryAutonomousExecutionAuthorizationStore(context: Context) {
    private val prefs by lazy {
        val app = context.applicationContext
        val key = MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            app, PREFS_NAME, key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Synchronized
    fun read(): VyomFactoryAutonomousExecutionAuthorization? = runCatching {
        if (!prefs.getBoolean("enabled", false)) return null
        VyomFactoryAutonomousExecutionAuthorization(
            configSha256 = prefs.getString("config_sha256", null) ?: return null,
            enabledAtMillis = prefs.getLong("enabled_at", 0L),
            expiresAtMillis = prefs.getLong("expires_at", 0L)
        )
    }.getOrNull()

    @Synchronized
    fun save(value: VyomFactoryAutonomousExecutionAuthorization) {
        check(prefs.edit()
            .putBoolean("enabled", true)
            .putString("config_sha256", value.configSha256)
            .putLong("enabled_at", value.enabledAtMillis)
            .putLong("expires_at", value.expiresAtMillis)
            .commit()) { "Autonomous execution authorization could not be stored" }
    }

    @Synchronized
    fun revoke() {
        check(prefs.edit().clear().commit()) { "Autonomous execution authorization could not be revoked" }
    }

    companion object { private const val PREFS_NAME = "vyom_factory_autonomous_execution_auth" }
}

internal object VyomFactoryAutonomousExecutionAuthorizationRuntime {
    fun currentConfig(context: Context): VyomFactoryAutonomousExecutionConfig {
        val app = context.applicationContext
        val binding = VyomFactorySystemRegistry(File(app.filesDir, "factory-system-registry"))
            .get(VyomFactoryWorkerBindingPolicy.SYSTEM_ID)
            ?: throw SecurityException("Bind the trusted worker repository before enabling autonomous execution")
        return VyomFactoryAutonomousExecutionConfig(
            repositoryUrl = binding.repositoryUrl,
            baseBranch = SelfModificationPolicy.PROPOSAL_BASE_BRANCH,
            baseCommitSha = binding.commitSha,
            allowedPaths = binding.allowedPaths
        ).also(VyomFactoryAutonomousExecutionAuthorizationPolicy::validate)
    }

    fun enable(context: Context, ownerConfirmed: Boolean, foregroundSession: Boolean, nowMillis: Long) {
        val config = currentConfig(context)
        val authorization = VyomFactoryAutonomousExecutionAuthorizationPolicy.enable(
            config, ownerConfirmed, foregroundSession, nowMillis
        )
        VyomFactoryAutonomousExecutionAuthorizationStore(context).save(authorization)
    }

    fun isEnabled(context: Context, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val store = VyomFactoryAutonomousExecutionAuthorizationStore(context)
        return try {
            val valid = VyomFactoryAutonomousExecutionAuthorizationPolicy.isValid(
                store.read(), currentConfig(context), nowMillis
            )
            if (!valid) store.revoke()
            valid
        } catch (_: Exception) {
            runCatching { store.revoke() }
            false
        }
    }

    fun disable(context: Context) {
        VyomFactoryAutonomousExecutionAuthorizationStore(context).revoke()
    }
}
