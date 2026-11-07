package com.hermes.assistant

import java.net.URI

/**
 * Pure acceptance policy for release manifests. The manifest and APK must come from
 * the same exact GitHub release tag; hashes and commit identity are mandatory.
 */
internal object UpdateManifestPolicy {
    data class Candidate(
        val versionCode: Int,
        val sha256: String,
        val certificateSha256: String,
        val commit: String,
        val releaseTag: String,
        val apkUrl: String
    )

    fun isWellFormed(candidate: Candidate): Boolean =
        candidate.versionCode > 0 &&
            candidate.sha256.matches(Regex("(?i)^[a-f0-9]{64}$")) &&
            candidate.certificateSha256.matches(Regex("(?i)^[a-f0-9]{64}$")) &&
            candidate.commit.matches(Regex("^[a-f0-9]{40}$")) &&
            isReleaseAssetUrl(candidate.apkUrl, candidate.releaseTag, "apk")

    fun matchesExpectedCommit(candidate: Candidate, expectedCommit: String): Boolean =
        expectedCommit.matches(Regex("^[a-f0-9]{40}$")) && candidate.commit == expectedCommit

    fun isNewerVersion(candidateVersionCode: Int, installedVersionCode: Int): Boolean =
        candidateVersionCode > installedVersionCode

    fun isReleaseAssetUrl(value: String, releaseTag: String, extension: String): Boolean {
        if (!releaseTag.matches(Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$"))) return false
        if (extension !in setOf("apk", "json")) return false
        return try {
            val uri = URI(value)
            if (!uri.scheme.equals("https", ignoreCase = true) ||
                !uri.host.equals("github.com", ignoreCase = true) ||
                uri.userInfo != null || uri.port != -1 ||
                uri.rawQuery != null || uri.rawFragment != null
            ) return false
            val segments = uri.path.split('/').filter(String::isNotEmpty)
            segments.size == 6 &&
                segments[0] == "riyas433554-dot" &&
                segments[1] == "vyom-android" &&
                segments[2] == "releases" &&
                segments[3] == "download" &&
                segments[4] == releaseTag &&
                segments[5].matches(Regex("^[A-Za-z0-9._-]+\\.$extension$"))
        } catch (_: Exception) {
            false
        }
    }
}
