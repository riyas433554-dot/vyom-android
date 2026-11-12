package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryWorkerLiveCallAuthorizationTest {
    private val config = VyomFactoryWorkerLiveCallConfig(
        providerId = NvidiaNemotronWorkerModelProvider.PROVIDER_ID,
        modelId = NvidiaNemotronWorkerModelProvider.DEFAULT_MODEL_ID,
        repositoryUrl = "https://github.com/example/worker-test",
        baseCommitSha = "a".repeat(40),
        readmeSha256 = "b".repeat(64),
        allowedPaths = listOf("README.md")
    )
    private val now = 1_800_000_000_000L

    @Test fun defaultOffWhenNoOwnerAuthorizationExists() {
        assertFalse(VyomFactoryWorkerLiveCallAuthorizationPolicy.isValid(null, config, now))
    }

    @Test fun ownerConfirmationAndForegroundSessionAreBothRequired() {
        assertThrows(IllegalStateException::class.java) {
            VyomFactoryWorkerLiveCallAuthorizationPolicy.enable(config, false, true, now)
        }
        assertThrows(IllegalStateException::class.java) {
            VyomFactoryWorkerLiveCallAuthorizationPolicy.enable(config, true, false, now)
        }
    }

    @Test fun authorizationExpiresAfterExactlyTwentyFourHours() {
        val record = VyomFactoryWorkerLiveCallAuthorizationPolicy.enable(config, true, true, now)
        assertEquals(now + VyomFactoryWorkerLiveCallAuthorizationPolicy.TTL_MILLIS, record.expiresAtMillis)
        assertTrue(VyomFactoryWorkerLiveCallAuthorizationPolicy.isValid(record, config, now + 1L))
        assertFalse(VyomFactoryWorkerLiveCallAuthorizationPolicy.isValid(
            record, config, now + VyomFactoryWorkerLiveCallAuthorizationPolicy.TTL_MILLIS
        ))
    }

    @Test fun anyProviderModelRepositoryCommitOrReadmeHashChangeInvalidatesAuthorization() {
        val record = VyomFactoryWorkerLiveCallAuthorizationPolicy.enable(config, true, true, now)
        val changed = listOf(
            config.copy(providerId = GoogleGemmaWorkerModelProvider.PROVIDER_ID),
            config.copy(modelId = "gemma-4-26b-a4b-it"),
            config.copy(repositoryUrl = "https://github.com/example/other-repo"),
            config.copy(baseCommitSha = "c".repeat(40)),
            config.copy(readmeSha256 = "d".repeat(64))
        )
        changed.forEach { current ->
            assertFalse(VyomFactoryWorkerLiveCallAuthorizationPolicy.isValid(record, current, now + 1L))
        }
    }

    @Test fun missingProviderModelRepositoryCommitOrReadmeHashIsRejected() {
        val invalid = listOf(
            config.copy(providerId = ""),
            config.copy(modelId = ""),
            config.copy(repositoryUrl = ""),
            config.copy(baseCommitSha = ""),
            config.copy(readmeSha256 = "")
        )
        invalid.forEach { candidate ->
            assertThrows(IllegalArgumentException::class.java) {
                VyomFactoryWorkerLiveCallAuthorizationPolicy.configSha256(candidate)
            }
        }
    }

    @Test fun authorizationBindsBothSafeKotlinRootsAndRejectsNarrowedOrChangedScopes() {
        val paths = listOf(
            "app/src/main/java/com/hermes/assistant",
            "app/src/test/java/com/hermes/assistant"
        )
        val expanded = config.copy(
            repositoryUrl = "https://github.com/riyas433554-dot/vyom-android",
            allowedPaths = paths
        )
        val record = VyomFactoryWorkerLiveCallAuthorizationPolicy.enable(expanded, true, true, now)
        assertTrue(VyomFactoryWorkerLiveCallAuthorizationPolicy.isValid(record, expanded, now + 1L))
        assertFalse(VyomFactoryWorkerLiveCallAuthorizationPolicy.isValid(
            record, expanded.copy(allowedPaths = listOf(paths.first())), now + 1L
        ))
    }

    @Test fun onlyTheExactReadmePathCanBeAuthorized() {
        for (paths in listOf(emptyList(), listOf("README.md", "docs"), listOf("README.md/child"), listOf("src"))) {
            assertThrows(IllegalArgumentException::class.java) {
                VyomFactoryWorkerLiveCallAuthorizationPolicy.configSha256(config.copy(allowedPaths = paths))
            }
        }
        assertTrue(VyomFactoryWorkerLiveCallAuthorizationPolicy.configSha256(config).matches(Regex("^[a-f0-9]{64}$")))
    }

    @Test fun releaseCertificateCannotBeTouchedByWorkerOrSelfModificationPlan() {
        assertEquals(listOf("README.md"), config.allowedPaths)
        assertFalse(config.allowedPaths.contains("config/release-certificate.sha256"))
        assertFalse(SelfModificationPolicy.isAllowedPath("config/release-certificate.sha256"))
    }
    @Test fun successfulReadmeVerificationIsCachedForAtMostSixtySeconds() {
        VyomFactoryWorkerReadmeVerificationCache.invalidate()
        var calls = 0
        val key = "provider|model|repo|commit|README.md"
        val hash = "c".repeat(64)
        assertEquals(hash, VyomFactoryWorkerReadmeVerificationCache.getOrFetch(key, now) { calls++; hash })
        assertEquals(hash, VyomFactoryWorkerReadmeVerificationCache.getOrFetch(key, now + 59_999L) { calls++; "d".repeat(64) })
        assertEquals(1, calls)
        assertEquals("d".repeat(64), VyomFactoryWorkerReadmeVerificationCache.getOrFetch(key, now + 60_000L) { calls++; "d".repeat(64) })
        assertEquals(2, calls)
    }
    @Test fun readmeVerificationFailureIsNotCachedAndConfigChangeInvalidatesCache() {
        VyomFactoryWorkerReadmeVerificationCache.invalidate()
        var calls = 0
        val key = "provider|model|repo|commit|README.md"
        assertThrows(IllegalStateException::class.java) {
            VyomFactoryWorkerReadmeVerificationCache.getOrFetch(key, now) { calls++; throw IllegalStateException("network") }
        }
        assertEquals("b".repeat(64), VyomFactoryWorkerReadmeVerificationCache.getOrFetch(key, now + 1L) { calls++; "b".repeat(64) })
        val changedKey = "provider|model2|repo|commit|README.md"
        assertEquals("e".repeat(64), VyomFactoryWorkerReadmeVerificationCache.getOrFetch(changedKey, now + 2L) { calls++; "e".repeat(64) })
        assertEquals(3, calls)
    }
    @Test fun invalidatedSuccessCannotMaskANewGithubNetworkFailure() {
        VyomFactoryWorkerReadmeVerificationCache.invalidate()
        val key = "provider|model|repo|commit|README.md"
        assertEquals("a".repeat(64), VyomFactoryWorkerReadmeVerificationCache.getOrFetch(key, now) { "a".repeat(64) })
        VyomFactoryWorkerReadmeVerificationCache.invalidate()
        assertThrows(IllegalStateException::class.java) {
            VyomFactoryWorkerReadmeVerificationCache.getOrFetch(key, now + 1L) {
                throw IllegalStateException("GitHub rate limit")
            }
        }
        assertThrows(IllegalStateException::class.java) {
            VyomFactoryWorkerReadmeVerificationCache.getOrFetch(key, now + 2L) {
                throw IllegalStateException("GitHub remains unavailable")
            }
        }
    }

    @Test fun authorizationRecordMustMatchItsStoredConfigurationHash() {
        val record = VyomFactoryWorkerLiveCallAuthorizationPolicy.enable(config, true, true, now)
        assertFalse(VyomFactoryWorkerLiveCallAuthorizationPolicy.isValid(
            record.copy(configSha256 = "0".repeat(64)), config, now + 1L
        ))
        assertFalse(VyomFactoryWorkerLiveCallAuthorizationPolicy.isValid(
            record.copy(allowedPath = "src"), config, now + 1L
        ))
    }

    @Test fun issueTextAndModelOutputAreNotAuthorizationInputs() {
        val method = VyomFactoryWorkerLiveCallAuthorizationPolicy::class.java.declaredMethods
            .single { it.name == "enable" }
        assertFalse(method.parameterTypes.any { it == String::class.java })
        assertFalse(method.parameterTypes.any { it == VyomFactoryGitHubClient.Task::class.java })
        assertFalse(method.parameterTypes.any { it == VyomFactoryWorkerModelProvider.Response::class.java })
    }
}
