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
}
