package com.hermes.assistant

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.TimeUnit

internal data class VyomFactoryWorkerLiveCallConfig(
    val providerId: String,
    val modelId: String,
    val repositoryUrl: String,
    val baseCommitSha: String,
    val readmeSha256: String,
    val allowedPaths: List<String>
)

internal data class VyomFactoryWorkerLiveCallAuthorization(
    val providerId: String,
    val modelId: String,
    val repositoryUrl: String,
    val baseCommitSha: String,
    val readmeSha256: String,
    val allowedPath: String,
    val configSha256: String,
    val enabledAtMillis: Long,
    val expiresAtMillis: Long
)

private fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

internal object VyomFactoryWorkerLiveCallAuthorizationPolicy {
    private val providers = setOf(
        NvidiaNemotronWorkerModelProvider.PROVIDER_ID,
        GoogleGemmaWorkerModelProvider.PROVIDER_ID
    )

    fun configSha256(config: VyomFactoryWorkerLiveCallConfig): String {
        validate(config)
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeUTF("VYOM-WORKER-LIVE-AUTH-V2")
            out.writeUTF(config.providerId)
            out.writeUTF(config.modelId)
            out.writeUTF(config.repositoryUrl)
            out.writeUTF(config.baseCommitSha)
            out.writeUTF(config.readmeSha256)
            out.writeInt(config.allowedPaths.size)
            config.allowedPaths.forEach(out::writeUTF)
        }
        return sha256Hex(bytes.toByteArray())
    }

    fun validate(config: VyomFactoryWorkerLiveCallConfig) {
        require(config.providerId in providers) { "Worker provider is missing or unsupported" }
        require(config.modelId.matches(Regex("^[A-Za-z0-9._/-]{3,160}$"))) { "Worker model ID is missing or invalid" }
        val parsedRepository = VyomFactoryRepositoryIntake.parseUrl(config.repositoryUrl)
        require(config.repositoryUrl == config.repositoryUrl.trim() &&
            parsedRepository != null && parsedRepository.canonicalUrl == config.repositoryUrl) {
            "Worker repository URL is missing or invalid"
        }
        require(config.baseCommitSha.matches(Regex("^[a-f0-9]{40}$"))) { "Worker base commit SHA is missing or invalid" }
        require(config.repositoryUrl == VyomFactoryWorkerBindingPolicy.TRUSTED_REPOSITORY_URL) {
            "Worker live calls are restricted to the trusted Vyom Android repository"
        }
        require(config.readmeSha256.matches(Regex("^[a-f0-9]{64}$"))) { "README.md SHA-256 is missing or invalid" }
        require(config.allowedPaths == VyomFactoryWorkerBindingPolicy.ALLOWED_PATHS) {
            "Worker live calls require the fixed safe Kotlin source/test roots"
        }
    }

    fun enable(
        config: VyomFactoryWorkerLiveCallConfig,
        ownerConfirmed: Boolean,
        foregroundSession: Boolean,
        nowMillis: Long
    ): VyomFactoryWorkerLiveCallAuthorization {
        check(ownerConfirmed) { "Owner confirmation is required to enable live model calls" }
        check(foregroundSession) { "Live model calls can only be enabled in the owner's foreground session" }
        require(nowMillis > 0L)
        val hash = configSha256(config)
        // Owner authorization persists until explicitly revoked or its bound config changes.
        // Long.MAX_VALUE is a sentinel, not a wall-clock expiry.
        return VyomFactoryWorkerLiveCallAuthorization(
            config.providerId, config.modelId, config.repositoryUrl, config.baseCommitSha,
            config.readmeSha256, config.allowedPaths.joinToString("|"), hash, nowMillis, Long.MAX_VALUE
        )
    }

    fun isValid(
        authorization: VyomFactoryWorkerLiveCallAuthorization?,
        currentConfig: VyomFactoryWorkerLiveCallConfig?,
        nowMillis: Long
    ): Boolean {
        if (authorization == null || currentConfig == null || nowMillis <= 0L) return false
        if (authorization.enabledAtMillis <= 0L || authorization.expiresAtMillis != Long.MAX_VALUE) return false
        val currentHash = runCatching { configSha256(currentConfig) }.getOrNull() ?: return false
        return authorization.providerId == currentConfig.providerId &&
            authorization.modelId == currentConfig.modelId &&
            authorization.repositoryUrl == currentConfig.repositoryUrl &&
            authorization.baseCommitSha == currentConfig.baseCommitSha &&
            authorization.readmeSha256 == currentConfig.readmeSha256 &&
            authorization.allowedPath == currentConfig.allowedPaths.joinToString("|") &&
            authorization.configSha256 == currentHash
    }
}

/** Encrypted at rest with an Android Keystore-backed master key. */
internal class VyomFactoryWorkerLiveCallAuthorizationStore(context: Context) {
    private val prefs by lazy {
        val app = context.applicationContext
        val masterKey = MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            app, PREFS_NAME, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Synchronized
    fun read(): VyomFactoryWorkerLiveCallAuthorization? = runCatching {
        if (!prefs.getBoolean("enabled", false)) return null
        VyomFactoryWorkerLiveCallAuthorization(
            providerId = prefs.getString("provider_id", null) ?: return null,
            modelId = prefs.getString("model_id", null) ?: return null,
            repositoryUrl = prefs.getString("repository_url", null) ?: return null,
            baseCommitSha = prefs.getString("base_commit_sha", null) ?: return null,
            readmeSha256 = prefs.getString("readme_sha256", null) ?: return null,
            allowedPath = prefs.getString("allowed_path", null) ?: return null,
            configSha256 = prefs.getString("config_sha256", null) ?: return null,
            enabledAtMillis = prefs.getLong("enabled_at", 0L),
            expiresAtMillis = prefs.getLong("expires_at", 0L)
        )
    }.getOrNull()

    @Synchronized
    fun save(value: VyomFactoryWorkerLiveCallAuthorization) {
        val editor = prefs.edit()
            .putBoolean("enabled", true)
            .putString("provider_id", value.providerId)
            .putString("model_id", value.modelId)
            .putString("repository_url", value.repositoryUrl)
            .putString("base_commit_sha", value.baseCommitSha)
            .putString("readme_sha256", value.readmeSha256)
            .putString("allowed_path", value.allowedPath)
            .putString("config_sha256", value.configSha256)
            .putLong("enabled_at", value.enabledAtMillis)
            .putLong("expires_at", value.expiresAtMillis)
        check(editor.commit()) { "Worker live-call authorization could not be stored" }
    }

    @Synchronized
    fun revoke() {
        check(prefs.edit().clear().commit()) { "Worker live-call authorization could not be revoked" }
    }

    companion object { private const val PREFS_NAME = "vyom_factory_worker_live_call_auth" }
}

/** Reads the README bytes at the exact immutable Git commit; no branch or mutable default ref is used. */
internal class VyomFactoryWorkerReadmeHashProvider(private val context: Context) {
    private val http = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build()

    fun readmeSha256(repositoryUrl: String, commitSha: String): String {
        val parts = repositoryUrl.removePrefix("https://github.com/").split("/")
        require(parts.size == 2 && commitSha.matches(Regex("^[a-f0-9]{40}$")))
        val app = context.applicationContext
        val masterKey = MasterKey.Builder(app).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        val prefs = EncryptedSharedPreferences.create(
            app, "vyom_voice_secrets", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val token = prefs.getString("github_worker_readme_token", "").orEmpty().trim()
        require(token.isNotBlank() && token.length <= 500) { "GitHub credential is unavailable for README verification" }
        val url = "https://api.github.com/repos/${parts[0]}/${parts[1]}/contents/README.md?ref=$commitSha"
        val request = Request.Builder().url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("Authorization", "Bearer $token")
            .get().build()
        http.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Exact-commit README verification failed (HTTP ${response.code})" }
            val responseBody = response.body ?: throw IllegalStateException("README API response is empty")
            val declaredLength = responseBody.contentLength()
            require(declaredLength == -1L || declaredLength in 1L..1_500_000L) { "README API response is too large" }
            val output = ByteArrayOutputStream()
            responseBody.byteStream().use { input ->
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= 1_500_000) { "README API response is too large" }
                    output.write(buffer, 0, count)
                }
            }
            val json = JSONObject(String(output.toByteArray(), Charsets.UTF_8))
            require(json.optString("path") == "README.md" && json.optString("type") == "file") {
                "Exact-commit README.md is missing or is not a regular file"
            }
            require(json.optString("encoding") == "base64") { "GitHub did not return base64 README content" }
            val encoded = json.getString("content")
            val bytes = Base64.getMimeDecoder().decode(encoded)
            require(bytes.isNotEmpty() && bytes.size <= 1_000_000) { "README.md is empty or exceeds its size limit" }
            return sha256Hex(bytes)
        }
    }
}

/** Process-local cache of successful immutable-commit README verification only. */
internal object VyomFactoryWorkerReadmeVerificationCache {
    private data class Verified(val key: String, val hash: String, val verifiedAtMillis: Long)
    private var verified: Verified? = null
    const val TTL_MILLIS = 60_000L
    @Synchronized fun getOrFetch(key: String, nowMillis: Long, fetch: () -> String): String {
        require(key.isNotBlank() && nowMillis > 0L)
        val previous = verified
        if (previous != null && previous.key != key) verified = null
        verified?.let {
            val age = nowMillis - it.verifiedAtMillis
            if (age in 0 until TTL_MILLIS) return it.hash
            verified = null
        }
        return try {
            val hash = fetch()
            require(hash.matches(Regex("^[a-f0-9]{64}$"))) { "README verification returned an invalid hash" }
            verified = Verified(key, hash, nowMillis)
            hash
        } catch (error: Exception) {
            verified = null
            throw error
        }
    }
    @Synchronized fun invalidate() { verified = null }
}

internal object VyomFactoryWorkerLiveCallAuthorizationRuntime {
    fun currentConfig(context: Context, readmeSha256: String): VyomFactoryWorkerLiveCallConfig {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(VyomFactoryWorkerModelRuntime.SETTINGS_PREFS, Context.MODE_PRIVATE)
        val provider = prefs.getString(VyomFactoryWorkerModelRuntime.SELECTED_PROVIDER, null)
            ?: throw SecurityException("Worker provider is not configured")
        val model = prefs.getString(provider + VyomFactoryWorkerModelRuntime.MODEL_ID_SUFFIX, null)
            ?: throw SecurityException("Worker model ID is not configured")
        val registry = VyomFactorySystemRegistry(File(app.filesDir, "factory-system-registry"))
        val binding = registry.get(VyomFactoryWorkerBindingPolicy.SYSTEM_ID)
            ?: throw SecurityException("Worker repository binding is missing")
        return VyomFactoryWorkerLiveCallConfig(
            provider, model, binding.repositoryUrl, binding.commitSha, readmeSha256, binding.allowedPaths
        ).also(VyomFactoryWorkerLiveCallAuthorizationPolicy::validate)
    }

    fun isEnabled(context: Context, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val app = context.applicationContext
        val store = VyomFactoryWorkerLiveCallAuthorizationStore(app)
        return try {
            val prefs = app.getSharedPreferences(VyomFactoryWorkerModelRuntime.SETTINGS_PREFS, Context.MODE_PRIVATE)
            val providerId = prefs.getString(VyomFactoryWorkerModelRuntime.SELECTED_PROVIDER, null)
                ?: throw SecurityException("Worker provider is missing")
            val key = VyomFactoryAndroidWorkerApiKeyStore(app, providerId).readKey()
                ?: throw SecurityException("Worker credential is missing")
            val hasKey = try { key.isNotEmpty() } finally { key.fill('\u0000') }
            if (!hasKey) throw SecurityException("Worker credential is missing")
            val binding = VyomFactorySystemRegistry(File(app.filesDir, "factory-system-registry"))
                .get(VyomFactoryWorkerBindingPolicy.SYSTEM_ID)
                ?: throw SecurityException("Worker repository binding is missing")
            val modelId = prefs.getString(providerId + VyomFactoryWorkerModelRuntime.MODEL_ID_SUFFIX, null)
                ?: throw SecurityException("Worker model ID is missing")
            val cacheKey = listOf(providerId, modelId, binding.repositoryUrl, binding.commitSha,
                binding.allowedPaths.joinToString(",")).joinToString("|")
            val readmeHash = VyomFactoryWorkerReadmeVerificationCache.getOrFetch(cacheKey, nowMillis) {
                VyomFactoryWorkerReadmeHashProvider(app)
                    .readmeSha256(binding.repositoryUrl, binding.commitSha)
            }
            val current = currentConfig(app, readmeHash)
            val valid = VyomFactoryWorkerLiveCallAuthorizationPolicy.isValid(store.read(), current, nowMillis)
            if (!valid) store.revoke()
            valid
        } catch (_: Exception) {
            runCatching { store.revoke() }
            false
        }
    }
}

/** Only the local foreground Settings control invokes this dialog; task/model data has no enable API. */
internal class VyomFactoryWorkerLiveCallAuthorizationDialog(
    private val activity: Activity,
    private val gate: VyomFactoryForegroundGate,
    private val onStatus: (String) -> Unit
) {
    fun disable() {
        VyomFactoryWorkerLiveCallAuthorizationStore(activity).revoke()
        onStatus("Worker live model calls are OFF.")
    }

    fun enable() {
        if (!gate.isForeground()) {
            onStatus("Enable requires the owner's foreground session.")
            return
        }
        val generation = gate.generation()
        onStatus("Verifying the immutable README sentinel at the bound commit…")
        Thread({
            try {
                val registry = VyomFactorySystemRegistry(File(activity.filesDir, "factory-system-registry"))
                val binding = registry.get(VyomFactoryWorkerBindingPolicy.SYSTEM_ID)
                    ?: throw SecurityException("Bind the trusted Vyom worker repository first")
                val readmeHash = VyomFactoryWorkerReadmeHashProvider(activity).readmeSha256(
                    binding.repositoryUrl, binding.commitSha
                )
                val config = VyomFactoryWorkerLiveCallAuthorizationRuntime.currentConfig(activity, readmeHash)
                val key = VyomFactoryAndroidWorkerApiKeyStore(activity, config.providerId).readKey()
                    ?: throw SecurityException("Save the selected provider credential first")
                val keyPresent = try { key.isNotEmpty() } finally { key.fill('\u0000') }
                if (!keyPresent) throw SecurityException("Save the selected provider credential first")
                activity.runOnUiThread {
                    if (!gate.isForegroundSession(generation)) {
                        onStatus("Enable cancelled because the foreground session ended.")
                        return@runOnUiThread
                    }
                    AlertDialog.Builder(activity)
                        .setTitle("ENABLE LIVE WORKER MODEL CALLS")
                        .setMessage(
                            "Provider: ${config.providerId}\nModel: ${config.modelId}\n" +
                                "Repository: ${config.repositoryUrl}\nCommit: ${config.baseCommitSha}\n" +
                                "README.md sentinel SHA-256: ${config.readmeSha256}\nAllowed roots: ${config.allowedPaths.joinToString(", ")}\n" +
                                "Factory runtime, safety/control, build, manifest, workflow and signing files are blocked.\n\n" +
                                "Calls remain enabled until the owner disables them. Any configuration/hash mismatch disables them. Model calls are made only when eligible tasks exist. Enable only for this exact binding?"
                        )
                        .setPositiveButton("ENABLE UNTIL DISABLED") { _, _ ->
                            try {
                                check(gate.isForegroundSession(generation)) {
                                    "Foreground owner confirmation session expired"
                                }
                                val current = VyomFactoryWorkerLiveCallAuthorizationRuntime.currentConfig(
                                    activity, config.readmeSha256
                                )
                                check(current == config) { "Worker settings changed during confirmation; authorization revoked" }
                                val authorization = VyomFactoryWorkerLiveCallAuthorizationPolicy.enable(
                                    config, ownerConfirmed = true, foregroundSession = true,
                                    nowMillis = System.currentTimeMillis()
                                )
                                VyomFactoryWorkerLiveCallAuthorizationStore(activity).save(authorization)
                                onStatus("Live worker model calls ENABLED until disabled · no eligible tasks means no model calls · owner review required.")
                            } catch (error: Exception) {
                                VyomFactoryWorkerLiveCallAuthorizationStore(activity).revoke()
                                onStatus("Live worker authorization rejected · ${error.message ?: error.javaClass.simpleName}")
                            }
                        }
                        .setNegativeButton("CANCEL", null)
                        .show()
                }
            } catch (error: Exception) {
                activity.runOnUiThread {
                    VyomFactoryWorkerLiveCallAuthorizationStore(activity).revoke()
                    onStatus("Live worker authorization unavailable · ${error.message ?: error.javaClass.simpleName}")
                }
            }
        }, "vyom-worker-auth-readme").start()
    }
}
