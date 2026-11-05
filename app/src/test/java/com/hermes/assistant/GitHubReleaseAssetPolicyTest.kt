package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class GitHubReleaseAssetPolicyTest {
    @Test fun buildsTheExactPrivateRepositoryReleaseAssetApiUrl() {
        assertEquals(
            "https://api.github.com/repos/riyas433554-dot/vyom-android/releases/assets/12345",
            GitHubReleaseAssetPolicy.assetApiUrl(12345L)
        )
    }

    @Test fun rejectsInvalidAssetIdsAndUntrustedApiPaths() {
        assertThrows(IllegalArgumentException::class.java) {
            GitHubReleaseAssetPolicy.assetApiUrl(0L)
        }
        assertFalse(GitHubReleaseAssetPolicy.isTrustedAssetApiUrl(
            "https://api.github.com/repos/attacker/repo/releases/assets/123"
        ))
        assertFalse(GitHubReleaseAssetPolicy.isTrustedAssetApiUrl(
            "https://api.github.com/repos/riyas433554-dot/vyom-android/issues/123"
        ))
    }

    @Test fun requiresSavedTokenAndBuildsBearerAuthorization() {
        assertEquals("Bearer ghp_example", GitHubReleaseAssetPolicy.authorizationHeader("ghp_example"))
        assertThrows(IllegalArgumentException::class.java) {
            GitHubReleaseAssetPolicy.authorizationHeader(" ")
        }
    }

    @Test fun acceptsOnlyHttpsGitHubReleaseAssetRedirectHosts() {
        assertTrue(GitHubReleaseAssetPolicy.isTrustedAssetRedirect(
            "https://release-assets.githubusercontent.com/github-production-release-asset/abc"
        ))
        assertTrue(GitHubReleaseAssetPolicy.isTrustedAssetRedirect(
            "https://objects.githubusercontent.com/github-production-release-asset/abc"
        ))
        assertFalse(GitHubReleaseAssetPolicy.isTrustedAssetRedirect(
            "https://attacker.example/payload"
        ))
        assertFalse(GitHubReleaseAssetPolicy.isTrustedAssetRedirect(
            "http://release-assets.githubusercontent.com/payload"
        ))
    }
}
