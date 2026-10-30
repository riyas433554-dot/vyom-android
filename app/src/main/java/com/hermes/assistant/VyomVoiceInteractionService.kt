package com.hermes.assistant

import android.service.voice.VoiceInteractionService

/** Android assistant role entry point. Invocation opens Vyom; it does not start hotword listening. */
class VyomVoiceInteractionService : VoiceInteractionService()
