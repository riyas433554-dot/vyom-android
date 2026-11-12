package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VyomFactoryRepositoryChangePolicyTest {
    private val repository = "https://github.com/riyas433554-dot/vyom-android"
    private val commit = "a".repeat(40)

    private fun registry(paths: List<String> = listOf("app/src/main/java/com/hermes/assistant", "app/src/test/java/com/hermes/assistant")): VyomFactorySystemRegistry {
        val root = File.createTempFile("vyom-worker-registry", ".tmp").apply {
            delete()
            mkdirs()
        }
        return VyomFactorySystemRegistry(root).also {
            it.register(
                VyomFactorySystemRegistry.Binding(
                    systemId = "factory-worker",
                    repositoryUrl = repository,
                    commitSha = commit,
                    allowedPaths = paths,
                    registeredAtEpochMs = 1L
                ),
                ownerConfirmed = true
            )
        }
    }

    private fun plan(
        changes: List<VyomFactoryRepositoryChangePolicy.Change> = listOf(
            VyomFactoryRepositoryChangePolicy.Change(
                "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt",
                "package com.hermes.assistant\n"
            )
        ),
        ownerApproved: Boolean = true,
        baseBranch: String = "feature/memory-intelligence",
        baseCommitSha: String = commit
    ) = VyomFactoryRepositoryChangePolicy.Plan(
        systemId = "factory-worker",
        issueNumber = 71,
        attemptId = "attempt-12345678",
        title = "Improve reply formatting",
        baseBranch = baseBranch,
        baseCommitSha = baseCommitSha,
        changes = changes,
        ownerApproved = ownerApproved
    )

    @Test fun acceptsOnlyARegisteredPinnedOwnerApprovedChangePlan() {
        val registry = registry()

        val binding = VyomFactoryRepositoryChangePolicy.validate(plan(), registry)

        assertEquals("factory-worker", binding.systemId)
        assertEquals(repository, binding.repositoryUrl)
        assertEquals(commit, binding.commitSha)
    }

    @Test fun refusesPlanWithoutExplicitOwnerApproval() {
        val error = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(plan(ownerApproved = false), registry())
        }.exceptionOrNull()

        assertTrue(error is SecurityException)
    }

    @Test fun refusesMissingSystemBinding() {
        val error = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(plan(), registry().let {
                VyomFactorySystemRegistry(File.createTempFile("empty-worker-registry", ".tmp").apply {
                    delete()
                    mkdirs()
                })
            })
        }.exceptionOrNull()

        assertTrue(error is SecurityException)
    }

    @Test fun refusesCommitThatDiffersFromRegisteredImmutableRevision() {
        val error = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(plan(baseCommitSha = "b".repeat(40)), registry())
        }.exceptionOrNull()

        assertTrue(error is SecurityException)
    }

    @Test fun refusesPathsOutsideSystemAllowedRoots() {
        val error = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(
                plan(changes = listOf(VyomFactoryRepositoryChangePolicy.Change("README.md", "change"))),
                registry()
            )
        }.exceptionOrNull()

        assertTrue(error is SecurityException)
    }

    @Test fun refusesTraversalAndDuplicatePaths() {
        val registry = registry()
        val traversal = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(
                plan(changes = listOf(VyomFactoryRepositoryChangePolicy.Change(
                    "app/src/main/java/com/hermes/assistant/../MainActivity.kt", "change"
                ))),
                registry
            )
        }.exceptionOrNull()
        val duplicate = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(
                plan(changes = listOf(
                    VyomFactoryRepositoryChangePolicy.Change("app/src/main/java/com/hermes/assistant/ReplyFormatter.kt", "one"),
                    VyomFactoryRepositoryChangePolicy.Change("app/src/main/java/com/hermes/assistant/ReplyFormatter.kt", "two")
                )),
                registry
            )
        }.exceptionOrNull()

        assertTrue(traversal is SecurityException || traversal is IllegalArgumentException)
        assertTrue(duplicate is IllegalArgumentException)
    }

    @Test fun refusesSecurityCriticalPathsEvenWhenRegistryRootAllowsThem() {
        val error = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(
                plan(changes = listOf(
                    VyomFactoryRepositoryChangePolicy.Change(
                        "app/src/main/java/com/hermes/assistant/VyomFactoryExecutionRunner.kt", "unsafe"
                    )
                )),
                registry()
            )
        }.exceptionOrNull()

        assertTrue(error is SecurityException)
    }

    @Test fun refusesMainBranchAndInvalidBranchNames() {
        val main = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(plan(baseBranch = "main"), registry())
        }.exceptionOrNull()
        val injection = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(plan(baseBranch = "feature/x;curl evil"), registry())
        }.exceptionOrNull()

        assertTrue(main is SecurityException)
        assertTrue(injection is IllegalArgumentException)
    }
 
    @Test fun refusesBuildScriptsAndWorkflowFilesEvenWhenRegistryRootAllowsThem() {
        val registry = registry(listOf("app", ".github", "scripts"))
        val buildScript = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(
                plan(changes = listOf(VyomFactoryRepositoryChangePolicy.Change("app/build.gradle.kts", "unsafe"))),
                registry
            )
        }.exceptionOrNull()
        val workflow = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(
                plan(changes = listOf(VyomFactoryRepositoryChangePolicy.Change(".github/workflows/ci.yml", "unsafe"))),
                registry
            )
        }.exceptionOrNull()
        val script = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(
                plan(changes = listOf(VyomFactoryRepositoryChangePolicy.Change("scripts/run-tests.sh", "unsafe"))),
                registry
            )
        }.exceptionOrNull()

        assertTrue(buildScript is SecurityException)
        assertTrue(workflow is SecurityException)
        assertTrue(script is SecurityException)
    }

    @Test fun refusesOversizedOrEmptyChangeSets() {
        val registry = registry()
        val empty = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(plan(changes = emptyList()), registry)
        }.exceptionOrNull()
        val huge = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(
                plan(changes = listOf(
                    VyomFactoryRepositoryChangePolicy.Change(
                        "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt",
                        "x".repeat(200_001)
                    )
                )),
                registry
            )
        }.exceptionOrNull()

        assertTrue(empty is IllegalArgumentException)
        assertTrue(huge is IllegalArgumentException)
    }

    @Test fun refusesMoreThanMaximumFileCount() {
        val registry = registry()
        val changes = (1..(VyomFactoryRepositoryChangePolicy.MAX_FILES + 1)).map { index ->
            VyomFactoryRepositoryChangePolicy.Change(
                "app/src/main/java/com/hermes/assistant/GeneratedLimitTest$index.kt",
                "content-$index"
            )
        }
        val error = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(plan(changes = changes), registry)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test fun refusesAggregateContentAboveMaximumAcrossIndividuallyValidFiles() {
        val registry = registry()
        val perFileChars = 166_667
        val changes = (1..3).map { index ->
            VyomFactoryRepositoryChangePolicy.Change(
                "app/src/main/java/com/hermes/assistant/GeneratedAggregateLimit$index.kt",
                "x".repeat(perFileChars)
            )
        }
        assertTrue(changes.all { it.content.length <= VyomFactoryRepositoryChangePolicy.MAX_FILE_CONTENT_CHARS })
        assertTrue(changes.sumOf { it.content.length } > VyomFactoryRepositoryChangePolicy.MAX_TOTAL_CONTENT_CHARS)

        val error = runCatching {
            VyomFactoryRepositoryChangePolicy.validate(plan(changes = changes), registry)
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test fun workerBindingAcceptsSafeKotlinSourceAndTestPathsButRejectsFactoryAndControlPaths() {
        val root = File.createTempFile("worker-kotlin-roots", ".tmp").apply { delete(); mkdirs() }
        try {
            val registry = VyomFactorySystemRegistry(root)
            val binding = VyomFactoryWorkerBindingPolicy.createBinding(repository, commit, 1L)
            VyomFactoryWorkerBindingPolicy.register(registry, binding, ownerConfirmed = true)
            val valid = plan(
                changes = listOf(VyomFactoryRepositoryChangePolicy.Change(
                    "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt", "package com.hermes.assistant"
                ))
            ).copy(systemId = "worker")
            assertEquals("worker", VyomFactoryRepositoryChangePolicy.validateCandidate(valid, registry).systemId)
            val testPlan = valid.copy(changes = listOf(VyomFactoryRepositoryChangePolicy.Change(
                "app/src/test/java/com/hermes/assistant/ReplyFormatterTest.kt", "package com.hermes.assistant"
            )))
            assertEquals("worker", VyomFactoryRepositoryChangePolicy.validateCandidate(testPlan, registry).systemId)
            for (path in listOf(
                "README.md",
                "app/src/main/java/com/hermes/assistant/MainActivity.kt",
                "app/src/main/java/com/hermes/assistant/VyomFactoryWorkerPoolDispatcher.kt",
                "app/build.gradle.kts"
            )) {
                val rejected = valid.copy(changes = listOf(VyomFactoryRepositoryChangePolicy.Change(path, "blocked")))
                assertTrue(path, runCatching {
                    VyomFactoryRepositoryChangePolicy.validateCandidate(rejected, registry)
                }.exceptionOrNull() is SecurityException)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun workerBindingAllowsOnlyExactReadmePathAtTheChangePolicyBoundary() {
        val root = File.createTempFile("worker-readme-only", ".tmp").apply { delete(); mkdirs() }
        try {
            val registry = VyomFactorySystemRegistry(root)
            val binding = VyomFactoryWorkerBindingPolicy.createBinding(
                "https://github.com/example/test-repo", commit, 1L
            )
            VyomFactoryWorkerBindingPolicy.register(registry, binding, ownerConfirmed = true)
            val valid = plan(
                changes = listOf(VyomFactoryRepositoryChangePolicy.Change("README.md", "test-only content"))
            ).copy(systemId = "worker")
            assertEquals("worker", VyomFactoryRepositoryChangePolicy.validateCandidate(valid, registry).systemId)
            for (path in listOf("README.md/child", "../README.md", "docs/README.md", "app/src/main/Secret.kt")) {
                val rejected = valid.copy(changes = listOf(
                    VyomFactoryRepositoryChangePolicy.Change(path, "must be rejected")
                ))
                assertTrue(path, runCatching {
                    VyomFactoryRepositoryChangePolicy.validateCandidate(rejected, registry)
                }.exceptionOrNull() is SecurityException)
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
