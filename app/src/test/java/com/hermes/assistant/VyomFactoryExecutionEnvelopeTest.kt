package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactoryExecutionEnvelopeTest {
    @Test fun legacySchemaOneIssueDefaultsToManualReview() {
        val body = "Please fix this bug.\n\n<!-- VYOM_FACTORY_META:{\"schema\":1,\"priority\":\"P1\",\"status\":\"open\",\"source\":\"factory\",\"source_id\":\"legacy-1\",\"idempotency_key\":\"factory:legacy-1\",\"events\":[]} -->"

        val envelope = VyomFactoryExecutionEnvelope.parse(body)

        assertEquals(VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW, envelope.target)
        assertEquals("Please fix this bug.", envelope.originalRequest)
        assertFalse(envelope.isExecutable)
    }

    @Test fun missingOrMalformedExecutionMarkerDefaultsToManualReview() {
        val missing = VyomFactoryExecutionEnvelope.parse("Do something")
        val malformed = VyomFactoryExecutionEnvelope.parse(
            "Do something\n<!-- VYOM_FACTORY_EXECUTION:{not-json} -->"
        )

        assertEquals(VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW, missing.target)
        assertEquals(VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW, malformed.target)
        assertFalse(missing.isExecutable)
        assertFalse(malformed.isExecutable)
    }

    @Test fun validAndroidActionEnvelopePreservesRequestAndArguments() {
        val body = """
            Original request: Open Settings and show Wi-Fi.
            <!-- VYOM_FACTORY_EXECUTION:{"schema":1,"target":"android_action","action":"open_settings","request":"Open Settings and show Wi-Fi.","arguments":{"screen":"wifi"}} -->
        """.trimIndent()

        val envelope = VyomFactoryExecutionEnvelope.parse(body)

        assertEquals(VyomFactoryExecutionEnvelope.Target.ANDROID_ACTION, envelope.target)
        assertEquals("Open Settings and show Wi-Fi.", envelope.originalRequest)
        assertEquals("open_settings", envelope.operation)
        assertEquals("wifi", envelope.arguments.optString("screen"))
        assertTrue(envelope.isExecutable)
    }

    @Test fun validRepositoryChangeEnvelopeRequiresNamedOperation() {
        val body = """
            Original request: Add a unit test.
            <!-- VYOM_FACTORY_EXECUTION:{"schema":1,"target":"repository_change","operation":"change_with_tests","request":"Add a unit test.","arguments":{"path":"app/src/test"}} -->
        """.trimIndent()

        val envelope = VyomFactoryExecutionEnvelope.parse(body)

        assertEquals(VyomFactoryExecutionEnvelope.Target.REPOSITORY_CHANGE, envelope.target)
        assertEquals("change_with_tests", envelope.operation)
        assertEquals("app/src/test", envelope.arguments.optString("path"))
        assertTrue(envelope.isExecutable)
    }

    @Test fun unknownTargetOrMissingOperationFailsClosed() {
        val unknown = VyomFactoryExecutionEnvelope.parse(
            """<!-- VYOM_FACTORY_EXECUTION:{"schema":1,"target":"shell","operation":"run","request":"x","arguments":{}} -->"""
        )
        val missingOperation = VyomFactoryExecutionEnvelope.parse(
            """<!-- VYOM_FACTORY_EXECUTION:{"schema":1,"target":"android_action","request":"x","arguments":{}} -->"""
        )

        assertEquals(VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW, unknown.target)
        assertEquals(VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW, missingOperation.target)
        assertFalse(unknown.isExecutable)
        assertFalse(missingOperation.isExecutable)
    }

    @Test fun invalidSchemaAndOversizedPayloadFailClosed() {
        val wrongSchema = VyomFactoryExecutionEnvelope.parse(
            """<!-- VYOM_FACTORY_EXECUTION:{"schema":2,"target":"android_action","action":"open_settings","request":"x","arguments":{}} -->"""
        )
        val oversized = VyomFactoryExecutionEnvelope.parse(
            "x".repeat(VyomFactoryExecutionEnvelope.MAX_BODY_LENGTH + 1)
        )

        assertEquals(VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW, wrongSchema.target)
        assertEquals(VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW, oversized.target)
    }

    @Test fun nullOrBlankBodyIsManualReview() {
        assertEquals(
            VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW,
            VyomFactoryExecutionEnvelope.parse(null).target
        )
        assertEquals(
            VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW,
            VyomFactoryExecutionEnvelope.parse("  ").target
        )
    }
    @Test fun duplicateExecutionMarkersFailClosed() {
        val marker = """<!-- VYOM_FACTORY_EXECUTION:{"schema":1,"target":"android_action","action":"open_settings","request":"x","arguments":{}} -->"""
        val envelope = VyomFactoryExecutionEnvelope.parse("$marker\\n$marker")

        assertEquals(VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW, envelope.target)
        assertFalse(envelope.isExecutable)
    }

    @Test fun oversizedArgumentsFailClosed() {
        val large = "x".repeat(8_100)
        val body = """<!-- VYOM_FACTORY_EXECUTION:{"schema":1,"target":"android_action","action":"open_settings","request":"x","arguments":{"data":"$large"}} -->"""
        val envelope = VyomFactoryExecutionEnvelope.parse(body)

        assertEquals(VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW, envelope.target)
        assertFalse(envelope.isExecutable)
    }

    @Test fun targetSpecificOperationFieldIsRequired() {
        val body = """<!-- VYOM_FACTORY_EXECUTION:{"schema":1,"target":"android_action","operation":"open_settings","request":"x","arguments":{}} -->"""
        val envelope = VyomFactoryExecutionEnvelope.parse(body)

        assertEquals(VyomFactoryExecutionEnvelope.Target.MANUAL_REVIEW, envelope.target)
        assertFalse(envelope.isExecutable)
    }

}
