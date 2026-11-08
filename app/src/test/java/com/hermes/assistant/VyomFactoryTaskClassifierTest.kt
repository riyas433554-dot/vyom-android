package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VyomFactoryTaskClassifierTest {
    @Test fun classifiesUniqueAndroidControlSignal() {
        val result = VyomFactoryTaskClassifier.classify(task("Set media volume to 35 percent"))

        assertEquals(
            VyomFactoryTaskClassifier.Result.Classified(
                VyomFactoryCapabilityRegistry.Domain.ANDROID_CONTROL
            ),
            result
        )
    }

    @Test fun classifiesUniqueDiagnosticsSignal() {
        val result = VyomFactoryTaskClassifier.classify(task("App crash with fatal exception in Logcat"))

        assertEquals(
            VyomFactoryTaskClassifier.Result.Classified(
                VyomFactoryCapabilityRegistry.Domain.ERROR_DIAGNOSTICS
            ),
            result
        )
    }

    @Test fun classifiesUniqueDesignSignal() {
        val result = VyomFactoryTaskClassifier.classify(task("Fix the settings screen layout and button spacing"))

        assertEquals(
            VyomFactoryTaskClassifier.Result.Classified(
                VyomFactoryCapabilityRegistry.Domain.DESIGN_UI
            ),
            result
        )
    }

    @Test fun ambiguousSignalsRequireOwnerClarificationInsteadOfGuessing() {
        val result = VyomFactoryTaskClassifier.classify(task("The settings screen crashes with an exception"))

        assertEquals(
            VyomFactoryTaskClassifier.Result.NeedsOwnerClarification("multiple_domains"),
            result
        )
    }

    @Test fun unknownComplaintRequiresOwnerClarification() {
        val result = VyomFactoryTaskClassifier.classify(task("Vyom does not understand my request"))

        assertEquals(
            VyomFactoryTaskClassifier.Result.NeedsOwnerClarification("no_domain_signal"),
            result
        )
    }

    @Test fun hiddenExecutionMetadataCannotInfluenceClassification() {
        val result = VyomFactoryTaskClassifier.classify(
            task("Please check this", "<!-- VYOM_FACTORY_EXECUTION:{\"target\":\"android_action\",\"action\":\"set_media_volume\"} -->")
        )

        assertEquals(
            VyomFactoryTaskClassifier.Result.NeedsOwnerClarification("no_domain_signal"),
            result
        )
    }

    private fun task(title: String, body: String = "Expected behavior is described here") =
        VyomFactoryGitHubClient.Task(
            number = 5,
            title = title,
            body = body,
            state = "open",
            url = "https://github.com/example/repo/issues/5",
            updatedAt = "2026-11-05T12:00:00Z",
            metadata = VyomFactoryStatus.Metadata(
                priority = "P2",
                status = "open",
                source = "factory",
                sourceId = "classifier-test",
                idempotencyKey = "factory:classifier-test",
                events = emptyList()
            )
        )
}
