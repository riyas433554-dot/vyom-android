package com.hermes.assistant

internal object ReleaseIdentityPolicy {
    fun matchesAny(installedFingerprints: Collection<String>, requiredFingerprint: String): Boolean {
        if (!requiredFingerprint.matches(Regex("^[a-f0-9]{64}$"))) return false
        return installedFingerprints.any { it.lowercase() == requiredFingerprint }
    }
}
