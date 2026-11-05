package com.hermes.assistant

import java.net.URI

/**
 * First boundary of the Universal Repository Integration Engine.
 *
 * It accepts repository snapshots from any supported source adapter, classifies
 * repository contents, and returns a review decision. It never clones, installs,
 * builds, imports, or executes repository content.
 */
internal object VyomFactoryRepositoryIntake {
    enum class Kind { SKILL, SOURCE_CODE, API_SPEC, WORKFLOW, DOCUMENTATION, UNKNOWN }
    enum class Decision { READY_FOR_REVIEW, NEEDS_OWNER_REVIEW, BLOCKED }

    data class SourceRef(
        val owner: String,
        val name: String,
        val canonicalUrl: String
    )

    data class Snapshot(
        val source: SourceRef,
        val defaultBranch: String,
        val commitSha: String,
        val licenseSpdx: String,
        val archived: Boolean,
        val treeTruncated: Boolean,
        val paths: List<String>
    )

    data class Assessment(
        val source: SourceRef,
        val immutableRef: String?,
        val kind: Kind,
        val decision: Decision,
        val readOnlyInspectionAllowed: Boolean,
        val sourceExecutionAllowed: Boolean = false,
        val installationApproved: Boolean = false,
        val reasons: List<String>
    )

    private val ownerPattern = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?$")
    private val repoPattern = Regex("^[A-Za-z0-9_.-]{1,100}$")
    private val shaPattern = Regex("^[a-f0-9]{40}$")
    private val branchPattern = Regex("^[A-Za-z0-9._/-]{1,200}$")
    private val allowedLicenses = setOf(
        "MIT", "Apache-2.0", "BSD-2-Clause", "BSD-3-Clause", "ISC", "CC0-1.0"
    )
    private val sourceManifests = setOf(
        "package.json", "Cargo.toml", "go.mod", "pyproject.toml", "setup.py",
        "pom.xml", "build.gradle", "build.gradle.kts", "settings.gradle",
        "settings.gradle.kts", "Gemfile", "composer.json", "pubspec.yaml"
    )
    private val apiSpecs = setOf(
        "openapi.yaml", "openapi.yml", "openapi.json",
        "swagger.yaml", "swagger.yml", "swagger.json"
    )

    fun parseUrl(value: String): SourceRef? = runCatching {
        val uri = URI(value.trim())
        if (uri.scheme != "https" || !uri.host.equals("github.com", ignoreCase = true)) return null
        if (uri.userInfo != null || uri.port != -1 || uri.query != null || uri.fragment != null) return null
        val segments = uri.path.orEmpty().trim('/').split('/')
        if (segments.size != 2) return null
        val owner = segments[0]
        val name = segments[1].removeSuffix(".git")
        if (!ownerPattern.matches(owner) || !repoPattern.matches(name) || name == "." || name == "..") return null
        SourceRef(owner, name, "https://github.com/$owner/$name")
    }.getOrNull()

    fun assess(snapshot: Snapshot): Assessment {
        val reasons = mutableListOf<String>()
        val sourceValid = ownerPattern.matches(snapshot.source.owner) &&
            repoPattern.matches(snapshot.source.name) &&
            snapshot.source.canonicalUrl == "https://github.com/${snapshot.source.owner}/${snapshot.source.name}"
        if (!sourceValid) reasons += "invalid_repository_identity"
        if (snapshot.archived) reasons += "repository_archived"
        if (snapshot.treeTruncated) reasons += "repository_tree_truncated"
        if (!branchPattern.matches(snapshot.defaultBranch) ||
            snapshot.defaultBranch.startsWith("/") ||
            snapshot.defaultBranch.contains("..") ||
            snapshot.defaultBranch.contains("//")
        ) reasons += "invalid_default_branch"
        if (!shaPattern.matches(snapshot.commitSha)) reasons += "missing_immutable_commit"
        if (snapshot.paths.isEmpty()) reasons += "repository_tree_empty"
        if (snapshot.paths.any { !isSafePath(it) }) reasons += "unsafe_repository_path"

        val kind = classify(snapshot.paths)
        if (kind == Kind.UNKNOWN) reasons += "repository_type_unknown"
        if (snapshot.licenseSpdx !in allowedLicenses) reasons += "license_requires_review"

        val hardBlocked = reasons.any {
            it in setOf(
                "invalid_repository_identity",
                "repository_archived",
                "repository_tree_truncated",
                "invalid_default_branch",
                "missing_immutable_commit",
                "repository_tree_empty",
                "unsafe_repository_path"
            )
        }
        val decision = when {
            hardBlocked -> Decision.BLOCKED
            reasons.isNotEmpty() -> Decision.NEEDS_OWNER_REVIEW
            else -> Decision.READY_FOR_REVIEW
        }
        return Assessment(
            source = snapshot.source,
            immutableRef = snapshot.commitSha.takeIf { shaPattern.matches(it) },
            kind = kind,
            decision = decision,
            readOnlyInspectionAllowed = !hardBlocked,
            sourceExecutionAllowed = false,
            installationApproved = false,
            reasons = reasons.distinct()
        )
    }

    fun classify(paths: List<String>): Kind {
        val safePaths = paths.filter(::isSafePath)
        val names = safePaths.map { it.substringAfterLast('/') }.toSet()
        return when {
            names.any { it.equals("SKILL.md", ignoreCase = true) } -> Kind.SKILL
            // A real source manifest is the primary repository type even when the
            // project also ships OpenAPI specs or CI workflows.
            names.any { it in sourceManifests } -> Kind.SOURCE_CODE
            names.any { it in apiSpecs } -> Kind.API_SPEC
            safePaths.any {
                it.startsWith(".github/workflows/") &&
                    (it.endsWith(".yml", true) || it.endsWith(".yaml", true))
            } -> Kind.WORKFLOW
            safePaths.any { it.equals("README.md", ignoreCase = true) || it.startsWith("docs/") } ->
                Kind.DOCUMENTATION
            else -> Kind.UNKNOWN
        }
    }

    private fun isSafePath(path: String): Boolean =
        path.isNotBlank() &&
            path.length <= 500 &&
            !path.startsWith("/") &&
            !path.contains('\\') &&
            !path.split('/').any { it.isBlank() || it == "." || it == ".." } &&
            path.none { it.isISOControl() }
}
