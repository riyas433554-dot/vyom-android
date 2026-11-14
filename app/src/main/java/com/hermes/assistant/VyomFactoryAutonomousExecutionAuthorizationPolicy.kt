package com.hermes.assistant

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest

internal data class VyomFactoryAutonomousExecutionConfig(
    val repositoryUrl: String,
    val baseBranch: String,
    val baseCommitSha: String,
    val allowedPaths: List<String>
)

internal data class VyomFactoryAutonomousExecutionAuthorization(
    val configSha256: String,
    val enabledAtMillis: Long,
    val expiresAtMillis: Long
)

/**
 * One-time owner consent for unattended, bounded repository proposals.
 * It is deliberately narrower than general repository or device automation.
 */
internal object VyomFactoryAutonomousExecutionAuthorizationPolicy {
    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    fun configSha256(config: VyomFactoryAutonomousExecutionConfig): String {
        validate(config)
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeUTF("VYOM-FACTORY-AUTONOMOUS-EXECUTION-V1")
            out.writeUTF(config.repositoryUrl)
            out.writeUTF(config.baseBranch)
            out.writeUTF(config.baseCommitSha)
            out.writeInt(config.allowedPaths.size)
            config.allowedPaths.forEach(out::writeUTF)
        }
        return sha256(bytes.toByteArray())
    }

    fun validate(config: VyomFactoryAutonomousExecutionConfig) {
        require(config.repositoryUrl == VyomFactoryWorkerBindingPolicy.TRUSTED_REPOSITORY_URL) {
            "Autonomous execution is restricted to the owner-bound Vyom Android repository"
        }
        require(config.baseBranch == VyomFactoryWorkerBindingPolicy.BASE_BRANCH &&
            config.baseBranch == SelfModificationPolicy.PROPOSAL_BASE_BRANCH) {
            "Autonomous execution is restricted to the approved Factory feature branch"
        }
        require(config.baseCommitSha.matches(Regex("^[a-f0-9]{40}$"))) {
            "Autonomous execution requires an exact immutable base commit"
        }
        require(config.allowedPaths == VyomFactoryWorkerBindingPolicy.ALLOWED_PATHS) {
            "Autonomous execution requires the fixed Kotlin source/test allowlist"
        }
    }

    fun enable(
        config: VyomFactoryAutonomousExecutionConfig,
        ownerConfirmed: Boolean,
        foregroundSession: Boolean,
        nowMillis: Long
    ): VyomFactoryAutonomousExecutionAuthorization {
        check(ownerConfirmed) { "Owner confirmation is required to enable autonomous execution" }
        check(foregroundSession) { "Autonomous execution can only be enabled in the owner's foreground session" }
        require(nowMillis > 0L)
        return VyomFactoryAutonomousExecutionAuthorization(
            configSha256 = configSha256(config),
            enabledAtMillis = nowMillis,
            expiresAtMillis = Long.MAX_VALUE
        )
    }

    fun isValid(
        authorization: VyomFactoryAutonomousExecutionAuthorization?,
        currentConfig: VyomFactoryAutonomousExecutionConfig?,
        nowMillis: Long
    ): Boolean {
        if (authorization == null || currentConfig == null || nowMillis <= 0L) return false
        if (authorization.enabledAtMillis <= 0L || authorization.expiresAtMillis != Long.MAX_VALUE) return false
        val currentHash = runCatching { configSha256(currentConfig) }.getOrNull() ?: return false
        return authorization.configSha256 == currentHash
    }
}
