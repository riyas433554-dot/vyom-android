package com.hermes.assistant

/** Process-local duplicate submission guard; server idempotency is specified separately in R1b. */
internal class ChatRequestGuard {
    private val inFlight = mutableSetOf<String>()
    @Synchronized fun tryStart(clientMessageId: String): Boolean =
        clientMessageId.isNotBlank() && inFlight.add(clientMessageId)
    @Synchronized fun finish(clientMessageId: String) { inFlight.remove(clientMessageId) }
}
