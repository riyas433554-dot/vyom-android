package com.hermes.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManifestPolicyTest {
    private val commit = "a".repeat(40)
    private val tag = "vyom-ci-$commit"
    private val candidate = UpdateManifestPolicy.Candidate(
        versionCode = 200,
        sha256 = "b".repeat(64),
        certificateSha256 = "c".repeat(64),
        commit = commit,
        releaseTag = tag,
        apkUrl = "https://github.com/riyas433554-dot/vyom-android/releases/download/$tag/vyom-0.3.1-signed.apk"
    )

    @Test fun acceptsAWellFormedManifestWhoseApkBelongsToTheSameRelease() {
        assertTrue(UpdateManifestPolicy.isWellFormed(candidate))
    }

    @Test fun rejectsAnApkAssetFromADifferentReleaseTag() {
        val otherTag = "vyom-ci-" + "d".repeat(40)
        assertFalse(UpdateManifestPolicy.isWellFormed(candidate.copy(
            apkUrl = candidate.apkUrl.replace(tag, otherTag)
        )))
    }

    @Test fun rejectsUntrustedHostsUserInfoAndNonHttpsUrls() {
        assertFalse(UpdateManifestPolicy.isReleaseAssetUrl(
            candidate.apkUrl.replace("github.com", "github.com.attacker.example"), tag, "apk"
        ))
        assertFalse(UpdateManifestPolicy.isReleaseAssetUrl(
            candidate.apkUrl.replace("https://", "https://attacker@"), tag, "apk"
        ))
        assertFalse(UpdateManifestPolicy.isReleaseAssetUrl(
            candidate.apkUrl.replace("https://", "http://"), tag, "apk"
        ))
    }

    @Test fun rejectsMalformedHashesAndCommitIds() {
        assertFalse(UpdateManifestPolicy.isWellFormed(candidate.copy(sha256 = "bad")))
        assertFalse(UpdateManifestPolicy.isWellFormed(candidate.copy(certificateSha256 = "bad")))
        assertFalse(UpdateManifestPolicy.isWellFormed(candidate.copy(commit = "short")))
    }

    @Test fun expectedProposalCommitMustMatchExactly() {
        assertTrue(UpdateManifestPolicy.matchesExpectedCommit(candidate, commit))
        assertFalse(UpdateManifestPolicy.matchesExpectedCommit(candidate, "d".repeat(40)))
        assertFalse(UpdateManifestPolicy.matchesExpectedCommit(candidate, "short"))
    }

    @Test fun onlyStrictlyNewerVersionCodesAreEligible() {
        assertTrue(UpdateManifestPolicy.isNewerVersion(200, 199))
        assertFalse(UpdateManifestPolicy.isNewerVersion(200, 200))
        assertFalse(UpdateManifestPolicy.isNewerVersion(199, 200))
    }

    @Test fun releaseAssetMustBeTheExpectedJsonOrApkFileForTheSameTag() {
        val manifestUrl = "https://github.com/riyas433554-dot/vyom-android/releases/download/$tag/vyom-update.json"
        assertTrue(UpdateManifestPolicy.isReleaseAssetUrl(manifestUrl, tag, "json"))
        assertFalse(UpdateManifestPolicy.isReleaseAssetUrl(manifestUrl, tag, "apk"))
        assertFalse(UpdateManifestPolicy.isReleaseAssetUrl(manifestUrl, "vyom-ci-" + "d".repeat(40), "json"))
        assertFalse(UpdateManifestPolicy.isReleaseAssetUrl(manifestUrl.replace("/download/", "/latest/"), tag, "json"))
    }
}
