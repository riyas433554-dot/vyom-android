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

    @Test fun includesOnlyGitHubErrorMessageAndRequestIdInDiagnostics() {
        val diagnostic = GitHubReleaseAssetPolicy.httpErrorMessage(
            404,
            """{"message":"Not Found","documentation_url":"https://docs.github.com/private","token":"must-not-appear"}""",
            "REQ_123:Abc"
        )
        assertEquals("GitHub release API HTTP 404: Not Found (request ID REQ_123:Abc)", diagnostic)
        assertFalse(diagnostic.contains("documentation_url"))
        assertFalse(diagnostic.contains("must-not-appear"))
    }

    @Test fun boundsAndSanitizesGitHubErrorDetails() {
        val diagnostic = GitHubReleaseAssetPolicy.httpErrorMessage(
            403,
            """{"message":"forbidden\nwith control"}""",
            "bad request id"
        )
        assertEquals("GitHub release API HTTP 403: forbidden with control", diagnostic)
        assertTrue(GitHubReleaseAssetPolicy.httpErrorMessage(404, "", null)
            .startsWith("GitHub release API HTTP 404"))
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
