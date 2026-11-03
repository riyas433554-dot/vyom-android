package com.hermes.assistant

/**
 * Best-effort cancellation at the trust boundary. A live remote binder may still
 * accept the harmless cancel operation after Shizuku owner authorization is lost.
 * Failure of one transaction must not prevent cancellation attempts for other work.
 */
internal object ShizukuRequestCancellation {
    private val requestIdPattern = Regex("^[A-Za-z0-9-]{1,64}$")

    fun cancelOutstanding(
        remoteBinderAlive: Boolean,
        requestIds: Collection<String>,
        cancel: (String) -> Unit
    ): Set<String> {
        if (!remoteBinderAlive) return emptySet()
        val attempted = linkedSetOf<String>()
        requestIds.forEach { requestId ->
            if (requestIdPattern.matches(requestId)) {
                attempted += requestId
                try { cancel(requestId) } catch (_: Exception) { }
            }
        }
        return attempted
    }
}
