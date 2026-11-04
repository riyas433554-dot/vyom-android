package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceStateMachineTest {
    @Test fun continuousTurnReturnsToListeningAfterSpeech() {
        val machine = VoiceStateMachine()
        assertEquals(VoiceState.LISTENING, machine.start())
        assertEquals(VoiceState.THINKING, machine.heardSpeech())
        assertEquals(VoiceState.SPEAKING, machine.replyReady())
        assertEquals(VoiceState.LISTENING, machine.speechFinished())
        assertEquals(VoiceState.THINKING, machine.heardSpeech())
    }

    @Test fun stopDisablesLoopAndSetsIdle() {
        val machine = VoiceStateMachine()
        machine.start()
        assertEquals(VoiceState.IDLE, machine.stop())
        assertEquals(false, machine.enabled)
        assertEquals(VoiceState.IDLE, machine.recognitionFailed())
    }

    @Test fun bargeInStopsSpeakingAndReturnsToListening() {
        val machine = VoiceStateMachine()
        machine.start()
        machine.heardSpeech()
        machine.replyReady()
        assertEquals(VoiceState.LISTENING, machine.bargeIn())
        assertEquals(true, machine.enabled)
    }

    @Test fun recognitionFailureKeepsContinuousModeEnabled() {
        val machine = VoiceStateMachine()
        machine.start()
        assertEquals(VoiceState.LISTENING, machine.recognitionFailed())
        assertEquals(true, machine.enabled)
    }
}