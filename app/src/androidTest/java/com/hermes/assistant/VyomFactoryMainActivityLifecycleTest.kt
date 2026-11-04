package com.hermes.assistant

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VyomFactoryMainActivityLifecycleTest {
    @Test
    fun pauseAndResumeInvalidatesThePreviouslyApprovedForegroundGeneration() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var approvedGeneration = -1L
            var approvalId = ""

            scenario.onActivity { activity ->
                assertTrue(activity.factoryForegroundGate.isForeground())
                approvedGeneration = activity.factoryForegroundGate.generation()
                approvalId = activity.factoryOneTimeApprovals.issue()
                assertTrue(approvalId.isNotBlank())
            }

            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity { activity ->
                assertFalse(activity.factoryForegroundGate.isForeground())
                assertFalse(activity.factoryForegroundGate.isForegroundSession(approvedGeneration))
                assertFalse(activity.factoryOneTimeApprovals.consume(approvalId))
            }

            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { activity ->
                assertTrue(activity.factoryForegroundGate.isForeground())
                assertFalse(activity.factoryForegroundGate.isForegroundSession(approvedGeneration))
                assertFalse(activity.factoryOneTimeApprovals.consume(approvalId))
            }
        }
    }
}
