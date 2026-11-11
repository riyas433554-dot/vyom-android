package com.hermes.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomApkUpdatePolicyTest {
    private val expectedCertificate = "f09e7db38a8eb4d95f036d495018912a62834bfbe9a3842ba2dfb58b8da2d762"
    private val apkSha = "a".repeat(64)
    private val releaseTag = "vyom-v0.3.2"

    private fun candidate(
        versionCode: Int = 12,
        sha256: String = apkSha,
        certificateSha256: String = expectedCertificate,
        apkUrl: String = "https://github.com/riyas433554-dot/vyom-android/releases/download/$releaseTag/vyom-0.3.2.apk"
    ) = UpdateManifestPolicy.Candidate(
        versionCode = versionCode,
        sha256 = sha256,
        certificateSha256 = certificateSha256,
        commit = "b".repeat(40),
        releaseTag = releaseTag,
        apkUrl = apkUrl
    )

    private fun archive(
        packageName: String = "com.vyom.assistant",
        versionCode: Long = 12L,
        sha256: String = apkSha,
        signerSha256: String = expectedCertificate
    ) = VyomApkUpdatePolicy.ArchiveIdentity(packageName, versionCode, sha256, signerSha256)

    @Test fun acceptsOnlyNewerApkWithExpectedPackageChecksumAndFixedSigner() {
        assertTrue(
            VyomApkUpdatePolicy.isAcceptable(
                candidate(), archive(),
                expectedPackage = "com.vyom.assistant",
                installedPackage = "com.vyom.assistant",
                installedVersionCode = 11L,
                expectedCertificateSha256 = expectedCertificate
            )
        )
    }

    @Test fun rejectsSignerMismatchEvenWhenManifestClaimsExpectedSigner() {
        assertFalse(
            VyomApkUpdatePolicy.isAcceptable(
                candidate(), archive(signerSha256 = "c".repeat(64)),
                "com.vyom.assistant", "com.vyom.assistant", 11L, expectedCertificate
            )
        )
    }

    @Test fun rejectsManifestCertificateMismatch() {
        assertFalse(
            VyomApkUpdatePolicy.isAcceptable(
                candidate(certificateSha256 = "c".repeat(64)), archive(),
                "com.vyom.assistant", "com.vyom.assistant", 11L, expectedCertificate
            )
        )
    }

    @Test fun rejectsWrongPackageAndInstalledPackageMismatch() {
        assertFalse(
            VyomApkUpdatePolicy.isAcceptable(
                candidate(), archive(packageName = "com.other.app"),
                "com.vyom.assistant", "com.vyom.assistant", 11L, expectedCertificate
            )
        )
        assertFalse(
            VyomApkUpdatePolicy.isAcceptable(
                candidate(), archive(),
                "com.vyom.assistant", "com.other.app", 11L, expectedCertificate
            )
        )
    }

    @Test fun rejectsDowngradeAndArchiveManifestVersionMismatch() {
        assertFalse(
            VyomApkUpdatePolicy.isAcceptable(
                candidate(versionCode = 10), archive(versionCode = 10L),
                "com.vyom.assistant", "com.vyom.assistant", 11L, expectedCertificate
            )
        )
        assertFalse(
            VyomApkUpdatePolicy.isAcceptable(
                candidate(versionCode = 12), archive(versionCode = 13L),
                "com.vyom.assistant", "com.vyom.assistant", 11L, expectedCertificate
            )
        )
    }

    @Test fun rejectsDownloadedChecksumMismatchAndUntrustedReleaseAssetUrl() {
        assertFalse(
            VyomApkUpdatePolicy.isAcceptable(
                candidate(), archive(sha256 = "d".repeat(64)),
                "com.vyom.assistant", "com.vyom.assistant", 11L, expectedCertificate
            )
        )
        assertFalse(
            VyomApkUpdatePolicy.isAcceptable(
                candidate(apkUrl = "https://evil.example/vyom.apk"), archive(),
                "com.vyom.assistant", "com.vyom.assistant", 11L, expectedCertificate
            )
        )
    }

    @Test fun usesLongVersionCodeOnAndroidPAndLater() {
        assertTrue(VyomApkUpdatePolicy.installedVersionCode(28, 4_294_967_300L, 7) == 4_294_967_300L)
    }

    @Test fun usesLegacyVersionCodeBeforeAndroidP() {
        assertTrue(VyomApkUpdatePolicy.installedVersionCode(27, 4_294_967_300L, 7) == 7L)
    }
}
