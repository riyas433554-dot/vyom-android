package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test

class VyomFactoryVerificationPolicyTest {
    private val assigned = "- **Verification probe:** Open the app and confirm the Factory screen shows the latest complaint"

    @Test fun extractsNamedProbeAndRejectsUnassignedPlaceholder() {
        assertEquals(
            "Open the app and confirm the Factory screen shows the latest complaint",
            VyomFactoryVerificationPolicy.assignedProbe(assigned)
        )
        assertNull(VyomFactoryVerificationPolicy.assignedProbe(
            "- **Verification probe:** UNASSIGNED at intake: define a named probe"
        ))
    }

    @Test fun onlyPassForTheExactCurrentProbeAllowsCompletion() {
        val probe = VyomFactoryVerificationPolicy.assignedProbe(assigned)!!
        val pass = VyomFactoryVerificationPolicy.resultEvent(probe, "Factory screen showed issue #42", true)
        val fail = VyomFactoryVerificationPolicy.resultEvent(probe, "Factory screen still blank", false)

        assertTrue(VyomFactoryVerificationPolicy.canComplete(assigned, listOf(pass)))
        assertFalse(VyomFactoryVerificationPolicy.canComplete(assigned, listOf(fail)))
        assertFalse(VyomFactoryVerificationPolicy.canComplete(assigned, emptyList()))
        assertFalse(VyomFactoryVerificationPolicy.canComplete(
            assigned.replace("latest complaint", "newest complaint"), listOf(pass)
        ))
    }

    @Test fun onlyNewCompletionRequiresProbePass() {
        assertTrue(VyomFactoryVerificationPolicy.blocksCompletion("in_progress", "completed", assigned, emptyList()))
        assertFalse(VyomFactoryVerificationPolicy.blocksCompletion(
            "in_progress", "completed", assigned,
            listOf(VyomFactoryVerificationPolicy.resultEvent(
                VyomFactoryVerificationPolicy.assignedProbe(assigned)!!, "Device showed the expected result", true
            ))
        ))
        assertFalse(VyomFactoryVerificationPolicy.blocksCompletion("completed", "completed", assigned, emptyList()))
        assertFalse(VyomFactoryVerificationPolicy.blocksCompletion("in_progress", "paused", assigned, emptyList()))
    }

    @Test fun unassignedProbeCannotBeCompletedEvenWithAnOldPassEvent() {
        val probe = VyomFactoryVerificationPolicy.assignedProbe(assigned)!!
        val pass = VyomFactoryVerificationPolicy.resultEvent(probe, "Owner checked it", true)
        assertFalse(VyomFactoryVerificationPolicy.canComplete(
            "- **Verification probe:** UNASSIGNED", listOf(pass)
        ))
    }
}
