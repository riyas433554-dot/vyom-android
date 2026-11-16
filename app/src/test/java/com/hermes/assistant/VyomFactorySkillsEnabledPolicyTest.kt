package com.hermes.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VyomFactorySkillsEnabledPolicyTest {
    @Test fun workerSkillsAreOffUntilOwnerExplicitlyEnablesThem() {
        assertFalse(VyomFactorySkillsEnabledPolicy.isEnabled(null))
        assertFalse(VyomFactorySkillsEnabledPolicy.isEnabled(false))
        assertTrue(VyomFactorySkillsEnabledPolicy.isEnabled(true))
        assertFalse(VyomFactorySkillsEnabledPolicy.DEFAULT_ENABLED)
    }
}
