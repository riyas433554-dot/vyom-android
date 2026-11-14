package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryAutonomousExecutionAuthorizationPolicyTest {
    private val config = VyomFactoryAutonomousExecutionConfig(
        repositoryUrl = "https://github.com/riyas433554-dot/vyom-android",
        baseBranch = "feature/real-worker-model",
        baseCommitSha = "a".repeat(40),
        allowedPaths = listOf(
            "app/src/main/java/com/hermes/assistant",
            "app/src/test/java/com/hermes/assistant"
        )
    )

    @Test fun ownerMustConfirmFromForegroundBeforeAutonomyCanBeEnabled() {
        assertTrue(runCatching {
            VyomFactoryAutonomousExecutionAuthorizationPolicy.enable(
                config, ownerConfirmed = false, foregroundSession = true, nowMillis = 100L
            )
        }.isFailure)
        assertTrue(runCatching {
            VyomFactoryAutonomousExecutionAuthorizationPolicy.enable(
                config, ownerConfirmed = true, foregroundSession = false, nowMillis = 100L
            )
        }.isFailure)
    }

    @Test fun authorizationIsBoundToExactRepositoryBranchCommitAndAllowedRoots() {
        val authorization = VyomFactoryAutonomousExecutionAuthorizationPolicy.enable(
            config, ownerConfirmed = true, foregroundSession = true, nowMillis = 100L
        )
        assertTrue(VyomFactoryAutonomousExecutionAuthorizationPolicy.isValid(authorization, config, 101L))
        assertFalse(VyomFactoryAutonomousExecutionAuthorizationPolicy.isValid(
            authorization, config.copy(baseCommitSha = "b".repeat(40)), 101L
        ))
        assertFalse(VyomFactoryAutonomousExecutionAuthorizationPolicy.isValid(
            authorization, config.copy(baseBranch = "main"), 101L
        ))
        assertFalse(VyomFactoryAutonomousExecutionAuthorizationPolicy.isValid(
            authorization, config.copy(allowedPaths = listOf("app/src/main")), 101L
        ))
    }

    @Test fun authorizationHasNoTwentyFourHourExpiryButCanBeInvalidatedByBindingChange() {
        val authorization = VyomFactoryAutonomousExecutionAuthorizationPolicy.enable(
            config, ownerConfirmed = true, foregroundSession = true, nowMillis = 100L
        )
        assertEquals(Long.MAX_VALUE, authorization.expiresAtMillis)
        assertTrue(VyomFactoryAutonomousExecutionAuthorizationPolicy.isValid(
            authorization, config, 100L + 365L * 24L * 60L * 60L * 1000L
        ))
    }

    @Test fun unsafeRepositoryOrProtectedBranchCannotBeAuthorized() {
        assertTrue(runCatching {
            VyomFactoryAutonomousExecutionAuthorizationPolicy.enable(
                config.copy(repositoryUrl = "https://github.com/attacker/repo"),
                ownerConfirmed = true, foregroundSession = true, nowMillis = 100L
            )
        }.isFailure)
        assertTrue(runCatching {
            VyomFactoryAutonomousExecutionAuthorizationPolicy.enable(
                config.copy(baseBranch = "main"),
                ownerConfirmed = true, foregroundSession = true, nowMillis = 100L
            )
        }.isFailure)
    }
}
