package com.hermes.assistant

/**
 * Curated, read-only discovery metadata for owner-supplied repositories.
 * This catalogue never clones, installs, imports, builds, or executes source.
 */
internal object VyomFactoryRepositoryCatalog {
    enum class Track { ANDROID_CONTROL, MEMORY, SECURITY_REFERENCE, VOICE_ASSISTANT }

    data class Candidate(
        val id: String,
        val canonicalUrl: String,
        val track: Track,
        val referenceOnly: Boolean = false,
        val licenseReviewRequired: Boolean = false,
        val executionAllowed: Boolean = false
    )

    val all: List<Candidate> = listOf(
        Candidate("anaay-jarvis", "https://github.com/AnaaySampat/jarvis", Track.ANDROID_CONTROL),
        Candidate("jarvis-os", "https://github.com/VarsshanCoder/JARVIS-OS-Agentic-AI-Assistant-for-Android", Track.ANDROID_CONTROL),
        Candidate("blurr", "https://github.com/Ayush0Chaudhary/blurr", Track.ANDROID_CONTROL, licenseReviewRequired = true),
        Candidate("aryan-jarvis", "https://github.com/Aryan-0001/Jarvis-AI", Track.VOICE_ASSISTANT),
        Candidate("mem0", "https://github.com/mem0ai/mem0", Track.MEMORY),
        Candidate("openjarvis", "https://github.com/open-jarvis/OpenJarvis", Track.MEMORY),
        Candidate("letta-code", "https://github.com/letta-ai/letta", Track.MEMORY),
        Candidate("isair-jarvis", "https://github.com/isair/jarvis", Track.VOICE_ASSISTANT, licenseReviewRequired = true),
        Candidate("h4cker", "https://github.com/The-Art-of-Hacking/h4cker", Track.SECURITY_REFERENCE, referenceOnly = true),
        Candidate("cybersecurity-ethical-hacking", "https://github.com/paulveillard/cybersecurity-ethical-hacking", Track.SECURITY_REFERENCE, referenceOnly = true),
        Candidate("payloads-all-the-things", "https://github.com/swisskyrepo/PayloadsAllTheThings", Track.SECURITY_REFERENCE, referenceOnly = true),
        Candidate("seclists", "https://github.com/danielmiessler/SecLists", Track.SECURITY_REFERENCE, referenceOnly = true),
        Candidate("strix", "https://github.com/usestrix/strix", Track.SECURITY_REFERENCE, referenceOnly = true),
        Candidate("shannon", "https://github.com/KeygraphHQ/shannon", Track.SECURITY_REFERENCE, referenceOnly = true, licenseReviewRequired = true),
        Candidate("pentestgpt", "https://github.com/GreyDGL/PentestGPT", Track.SECURITY_REFERENCE, referenceOnly = true),
        Candidate("building-jarvis-4", "https://github.com/arthi-rajendran24/building-jarvis-4.0", Track.VOICE_ASSISTANT, licenseReviewRequired = true),
        Candidate("ethanplusai-jarvis", "https://github.com/ethanplusai/jarvis", Track.VOICE_ASSISTANT, licenseReviewRequired = true)
    )

    fun byTrack(track: Track): List<Candidate> = all.filter { it.track == track }
}
