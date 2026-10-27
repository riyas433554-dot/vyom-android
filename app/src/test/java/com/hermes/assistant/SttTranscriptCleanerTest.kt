package com.hermes.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class SttTranscriptCleanerTest {
    @Test fun removesAudioEventTokensAndAnnotations() {
        assertEquals("Turn left now", SttTranscriptCleaner.clean("Turn [background noise] left <|audio_event|> now"))
    }

    @Test fun preservesOrdinarySpokenWordsAndNormalizesWhitespace() {
        assertEquals("hello there", SttTranscriptCleaner.clean(" hello   there "))
    }
}
