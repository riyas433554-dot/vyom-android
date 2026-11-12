package com.hermes.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfModificationPolicyTest {
    @Test fun allowsOrdinaryAppImplementationFiles() {
        assertTrue(SelfModificationPolicy.isAllowedPath("app/src/main/java/com/hermes/assistant/NemotronLiveClient.kt"))
        assertTrue(SelfModificationPolicy.isAllowedPath("app/src/main/java/com/hermes/assistant/feature/ReplyFormatter.kt"))
    }

    @Test fun rejectsSafetyAndControlFiles() {
        assertFalse(SelfModificationPolicy.isAllowedPath("app/src/main/java/com/hermes/assistant/MainActivity.kt"))
        assertFalse(SelfModificationPolicy.isAllowedPath("app/src/main/java/com/hermes/assistant/ActionEngine.kt"))
        assertFalse(SelfModificationPolicy.isAllowedPath("app/src/main/java/com/hermes/assistant/DeviceActionExecutor.kt"))
        assertFalse(SelfModificationPolicy.isAllowedPath("app/src/main/java/com/hermes/assistant/VoiceForegroundService.kt"))
        assertFalse(SelfModificationPolicy.isAllowedPath("app/src/main/java/com/hermes/assistant/SelfModificationPolicy.kt"))
        assertFalse(SelfModificationPolicy.isAllowedPath("app/src/main/java/com/hermes/assistant/UpdateVerifier.kt"))
        assertFalse(SelfModificationPolicy.isAllowedPath("app/src/main/java/com/hermes/assistant/ReleaseSigning.kt"))
        assertFalse(SelfModificationPolicy.isAllowedPath("app/src/main/java/com/hermes/assistant/UiBundleEditor.kt"))
        assertFalse(SelfModificationPolicy.isAllowedPath("app/src/main/java/com/hermes/assistant/VyomShizukuUserService.kt"))
    }

    @Test fun allowsUnitTestSourcesButProtectsFactoryWorkerInfrastructure() {
        assertTrue(SelfModificationPolicy.isAllowedWorkerPath(
            "app/src/test/java/com/hermes/assistant/ReplyFormatterTest.kt"
        ))
        assertFalse(SelfModificationPolicy.isAllowedPath(
            "app/src/main/java/com/hermes/assistant/VyomFactoryWorkerPoolDispatcher.kt"
        ))
        assertFalse(SelfModificationPolicy.isAllowedWorkerPath(
            "app/src/test/java/com/hermes/assistant/VyomFactoryWorkerPoolDispatchTest.kt"
        ))
    }

    @Test fun releaseCertificateConfigIsExplicitlyProtectedFromSelfModification() {
        assertTrue(SelfModificationPolicy.isExplicitlyProtectedRepositoryPath("config/release-certificate.sha256"))
        assertFalse(SelfModificationPolicy.isAllowedPath("config/release-certificate.sha256"))
        assertFalse(SelfModificationPolicy.isAllowedPath("config/../config/release-certificate.sha256"))
    }

    @Test fun rejectsTraversalAndNonSourcePaths() {
        assertFalse(SelfModificationPolicy.isAllowedPath("app/src/main/java/com/hermes/assistant/../MainActivity.kt"))
        assertFalse(SelfModificationPolicy.isAllowedPath(".github/workflows/unsafe.yml"))
        assertFalse(SelfModificationPolicy.isAllowedPath("app/build.gradle.kts"))
    }

    @Test fun validatesTitleAndNonEmptyBoundedContent() {
        SelfModificationPolicy.validate("Improve reply formatting", "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt", "package com.hermes.assistant\n")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsProtectedPathDuringValidation() {
        SelfModificationPolicy.validate("Improve reply formatting", "app/src/main/java/com/hermes/assistant/MainActivity.kt", "package com.hermes.assistant")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankContent() {
        SelfModificationPolicy.validate("Improve reply formatting", "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt", "  ")
    }

    @Test fun proposalBaseBranchTargetsTheActiveWorkerBranch() {
        assertTrue(SelfModificationPolicy.PROPOSAL_BASE_BRANCH == "feature/real-worker-model")
    }


    @Test fun acceptsBoundedSelfUpdateTaskForAnAllowedSourceFile() {
        SelfModificationPolicy.validateTask(
            "Improve reply formatting",
            "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt",
            "Improve Markdown list rendering and preserve existing public APIs."
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankSelfUpdateTask() {
        SelfModificationPolicy.validateTask(
            "Improve reply formatting",
            "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt",
            "   "
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSensitiveSelfUpdateTaskText() {
        SelfModificationPolicy.validateTask(
            "Improve reply formatting",
            "app/src/main/java/com/hermes/assistant/ReplyFormatter.kt",
            "Hardcode this API key: sk-12345678901234567890"
        )
    }

}
