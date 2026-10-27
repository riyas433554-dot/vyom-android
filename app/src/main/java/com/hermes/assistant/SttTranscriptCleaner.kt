package com.hermes.assistant

/** Removes recognizer annotations that should never be treated as spoken words. */
internal object SttTranscriptCleaner {
    fun clean(raw: String): String = raw
        .replace(Regex("""(?i)<\|(?:audio_event|audio_perception|soft_audio|text_sentinel|end_of_task)\|>"""), " ")
        .replace(Regex("""(?i)\[(?:noise|music|laughter|laughing|sigh|cough|door slam|background noise|audio event|applause|unintelligible speech)[^]]*]"""), " ")
        .replace(Regex("""(?i)\((?:noise|music|laughter|laughing|sigh|cough|door slam|background noise|audio event|applause|unintelligible speech)[^)]*\)"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}
