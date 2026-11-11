package com.hermes.assistant

/** Fail-closed APK archive checks before an in-place PackageInstaller update. */
internal object VyomApkUpdatePolicy {
    data class ArchiveIdentity(
        val packageName: String,
        val versionCode: Long,
        val sha256: String,
        val signerSha256: String
    )

    fun isAcceptable(
        candidate: UpdateManifestPolicy.Candidate,
        archive: ArchiveIdentity,
        expectedPackage: String,
        installedPackage: String,
        installedVersionCode: Long,
        expectedCertificateSha256: String
    ): Boolean = runCatching {
        require(expectedPackage.isNotBlank() && installedPackage == expectedPackage)
        require(UpdateManifestPolicy.isWellFormed(candidate))
        require(expectedCertificateSha256.matches(Regex("(?i)^[a-f0-9]{64}$")))
        require(candidate.certificateSha256.equals(expectedCertificateSha256, ignoreCase = true))
        require(archive.packageName == expectedPackage)
        require(archive.versionCode == candidate.versionCode.toLong())
        require(candidate.versionCode.toLong() > installedVersionCode)
        require(archive.sha256.matches(Regex("(?i)^[a-f0-9]{64}$")))
        require(archive.sha256.equals(candidate.sha256, ignoreCase = true))
        require(archive.signerSha256.matches(Regex("(?i)^[a-f0-9]{64}$")))
        require(archive.signerSha256.equals(expectedCertificateSha256, ignoreCase = true))
        true
    }.getOrDefault(false)
}
