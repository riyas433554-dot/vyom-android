package com.hermes.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomMemoryPrincipleTest {
    @Test fun learnBeforeActPreferenceIsDurableAndNonSensitive() {
        val fact = VyomMemoryStore.CORE_LEARN_BEFORE_ACT
        assertTrue(fact.contains("learn from trusted documentation/repositories"))
        assertTrue(fact.contains("review license/source/dependencies/security"))
        assertTrue(fact.contains("test safely"))
        assertTrue(fact.contains("report only verified results"))
        assertFalse(MemoryCapturePolicy.isSensitive(fact))
    }
}
