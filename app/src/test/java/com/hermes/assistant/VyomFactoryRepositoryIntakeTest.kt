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
