package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryExecutionPermitTest {
    @Test fun onlyOneRunnerCanOwnTheProcessPermitAtATime() {
        val permit = VyomFactoryExecutionPermit()
        assertTrue(permit.tryAcquire())
        assertFalse(permit.tryAcquire())
        permit.release()
        assertTrue(permit.tryAcquire())
        permit.release()
    }
}
