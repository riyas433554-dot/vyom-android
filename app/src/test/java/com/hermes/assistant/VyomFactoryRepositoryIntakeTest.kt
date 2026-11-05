package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryRepositoryIntakeTest {
    @Test fun parsesOnlyCanonicalGitHubRepositoryUrls() {
        assertEquals(
            VyomFactoryRepositoryIntake.SourceRef("openai", "example", "https://github.com/openai/example"),
            VyomFactoryRepositoryIntake.parseUrl("https://github.com/openai/example.git")
        )
        assertEquals(null, VyomFactoryRepositoryIntake.parseUrl("https://github.com.evil.example/openai/example"))
        assertEquals(null, VyomFactoryRepositoryIntake.parseUrl("https://github.com/openai/example/issues/1"))
        assertEquals(null, VyomFactoryRepositoryIntake.parseUrl("https://user:pass@github.com/openai/example"))
        assertEquals(null, VyomFactoryRepositoryIntake.parseUrl("http://github.com/openai/example"))
    }

    @Test fun classifiesDifferentRepositoryTypesWithoutAssumingSkillMd() {
        assertEquals(
            VyomFactoryRepositoryIntake.Kind.SKILL,
            assess(paths = listOf("SKILL.md", "README.md")).kind
        )
        assertEquals(
            VyomFactoryRepositoryIntake.Kind.SOURCE_CODE,
            assess(paths = listOf("README.md", "package.json", "src/index.ts")).kind
        )
        assertEquals(
            VyomFactoryRepositoryIntake.Kind.API_SPEC,
            assess(paths = listOf("openapi.yaml", "README.md")).kind
        )
        assertEquals(
            VyomFactoryRepositoryIntake.Kind.WORKFLOW,
            assess(paths = listOf(".github/workflows/ci.yml", "README.md")).kind
        )
        assertEquals(
            VyomFactoryRepositoryIntake.Kind.DOCUMENTATION,
            assess(paths = listOf("README.md", "docs/guide.md")).kind
        )
    }

    @Test fun unknownLicenseRequiresOwnerReviewAndNeverEnablesExecution() {
        val assessment = assess(license = "NOASSERTION", paths = listOf("README.md", "package.json"))

        assertEquals(VyomFactoryRepositoryIntake.Decision.NEEDS_OWNER_REVIEW, assessment.decision)
        assertTrue(assessment.readOnlyInspectionAllowed)
        assertFalse(assessment.sourceExecutionAllowed)
        assertTrue(assessment.reasons.isNotEmpty())
    }

    @Test fun archivedOrTruncatedRepositoriesAreBlocked() {
        assertEquals(
            VyomFactoryRepositoryIntake.Decision.BLOCKED,
            assess(archived = true).decision
        )
        assertEquals(
            VyomFactoryRepositoryIntake.Decision.BLOCKED,
            assess(truncated = true).decision
        )
    }

    @Test fun acceptedSourceRepositoryIsReviewOnlyAndNotExecutable() {
        val assessment = assess(paths = listOf("README.md", "build.gradle.kts", "src/Main.kt"))

        assertEquals(VyomFactoryRepositoryIntake.Decision.READY_FOR_REVIEW, assessment.decision)
        assertEquals(VyomFactoryRepositoryIntake.Kind.SOURCE_CODE, assessment.kind)
        assertTrue(assessment.readOnlyInspectionAllowed)
        assertFalse(assessment.sourceExecutionAllowed)
        assertFalse(assessment.installationApproved)
    }


    @Test fun repositoryClientPinsTreeInspectionToResolvedImmutableCommit() {
        val commit = "b".repeat(40)
        val requests = mutableListOf<String>()
        val client = VyomFactoryRepositoryClient(
            getJson = { requestPath ->
                requests += requestPath
                when {
                    requestPath == "/repos/openai/example" ->
                        """{"default_branch":"main","archived":false,"size":123,"description":"Example source","stargazers_count":42,"updated_at":"2026-11-05T00:00:00Z","license":{"spdx_id":"MIT"}}"""
                    requestPath == "/repos/openai/example/commits?sha=main&per_page=1" ->
                        """[{"sha":"$commit"}]"""
                    requestPath == "/repos/openai/example/git/trees/$commit?recursive=1" ->
                        """{"truncated":false,"tree":[{"path":"README.md","type":"blob","size":10},{"path":"package.json","type":"blob","size":20}]}"""
                    else -> throw AssertionError("Unexpected GitHub request: $requestPath")
                }
            }
        )

        val preview = client.inspect("https://github.com/openai/example")

        assertEquals(commit, preview.snapshot.commitSha)
        assertEquals(VyomFactoryRepositoryIntake.Kind.SOURCE_CODE, preview.assessment.kind)
        assertEquals(VyomFactoryRepositoryIntake.Decision.READY_FOR_REVIEW, preview.assessment.decision)
        assertTrue(requests.contains("/repos/openai/example/git/trees/$commit?recursive=1"))
        assertFalse(preview.assessment.sourceExecutionAllowed)
        assertFalse(preview.assessment.installationApproved)
    }


    @Test fun filePreviewUsesPinnedCommitAndRedactsRecognizedSecrets() {
        val commit = "c".repeat(40)
        val raw = """# Example

github token: ghp_ABCDEFGHIJKLMNOPQRSTUVWXYZ123456
api_key = "super-secret-value-123456789"
"""
        val encoded = java.util.Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8))
        val client = VyomFactoryRepositoryClient(
            getJson = { requestPath ->
                assertEquals("/repos/openai/example/contents/README.md?ref=$commit", requestPath)
                """{"type":"file","encoding":"base64","size":${raw.toByteArray(Charsets.UTF_8).size},"path":"README.md","sha":"blob-sha","content":"$encoded"}"""
            }
        )
        val source = VyomFactoryRepositoryIntake.SourceRef(
            "openai", "example", "https://github.com/openai/example"
        )
        val snapshot = VyomFactoryRepositoryIntake.Snapshot(
            source, "main", commit, "MIT", false, false, listOf("README.md")
        )
        val repository = VyomFactoryRepositoryClient.Preview(
            snapshot, VyomFactoryRepositoryIntake.assess(snapshot), "Example", 1, 1, "2026-11-05"
        )

        val files = client.previewFiles(repository)

        assertEquals(1, files.size)
        assertEquals("README.md", files.single().path)
        assertEquals(2, files.single().redactions)
        assertFalse(files.single().content.contains("ghp_ABCDEFGHIJKLMNOPQRSTUVWXYZ123456"))
        assertFalse(files.single().content.contains("super-secret-value-123456789"))
        assertTrue(files.single().untrusted)
    }

    private fun assess(
        license: String = "MIT",
        archived: Boolean = false,
        truncated: Boolean = false,
        paths: List<String> = listOf("README.md", "package.json")
    ) = VyomFactoryRepositoryIntake.assess(
        VyomFactoryRepositoryIntake.Snapshot(
            source = VyomFactoryRepositoryIntake.SourceRef(
                owner = "openai",
                name = "example",
                canonicalUrl = "https://github.com/openai/example"
            ),
            defaultBranch = "main",
            commitSha = "a".repeat(40),
            licenseSpdx = license,
            archived = archived,
            treeTruncated = truncated,
            paths = paths
        )
    )
}
