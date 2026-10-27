package com.hermes.assistant

enum class VoiceState { IDLE, LISTENING, THINKING, SPEAKING }

class VoiceStateMachine {
    var state: VoiceState = VoiceState.IDLE
        private set
    var enabled: Boolean = false
        private set

    fun start(): VoiceState { enabled = true; state = VoiceState.LISTENING; return state }
    fun heardSpeech(): VoiceState { if (enabled && state == VoiceState.LISTENING) state = VoiceState.THINKING; return state }
    fun replyReady(): VoiceState { if (enabled && state == VoiceState.THINKING) state = VoiceState.SPEAKING; return state }
    fun speechFinished(): VoiceState { if (enabled && state == VoiceState.SPEAKING) state = VoiceState.LISTENING; return state }
    fun bargeIn(): VoiceState { if (enabled && state == VoiceState.SPEAKING) state = VoiceState.LISTENING; return state }
    fun stop(): VoiceState { enabled = false; state = VoiceState.IDLE; return state }
    fun recognitionFailed(): VoiceState { state = if (enabled) VoiceState.LISTENING else VoiceState.IDLE; return state }
}