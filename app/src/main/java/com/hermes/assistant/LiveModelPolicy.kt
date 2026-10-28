package com.hermes.assistant

/** LIVE lane model policy: default first, and fallback only when the provider says HTTP 404. */
internal object LiveModelPolicy {
    const val DEFAULT_MODEL = "nvidia/nemotron-3.5-lightning-30b-a3b"
    const val FALLBACK_MODEL = "nemotron-3.5-bolt-e4b"

    fun selectedOrDefault(savedModel: String?): String =
        savedModel?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_MODEL

    fun fallbackFor(httpStatus: Int, requestedModel: String): String? =
        if (httpStatus == 404 && requestedModel != FALLBACK_MODEL) FALLBACK_MODEL else null
}
