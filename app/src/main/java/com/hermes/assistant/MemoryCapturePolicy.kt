package com.hermes.assistant
internal object MemoryCapturePolicy {
    private const val MAX_INPUT_CHARS = 2_000
    private const val MAX_FACT_CHARS = 140
    private val sensitivePatterns = listOf(
        Regex("(?i)\\b(password|passcode|otp|one[- ]time password|verification code|security code|pin|upi pin|cvv|cvc)\\b"),
        Regex("(?i)\\b(api key|secret key|access token|refresh token|bearer token|private key|recovery phrase|seed phrase)\\b"),
        Regex("(?i)\\b(bank account|account number|routing number|credit card|debit card|card number|wallet address)\\b"),
        Regex("(?i)\\b(aadhaar|aadhar|pan number|passport number|social security number|ssn)\\b"),
        Regex("(?i)\\b(medical diagnosis|medication|prescription|health condition)\\b"),
        Regex("(?i)\\b\\d{13,19}\\b"),
        Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE),
        Regex("\\b(?:\\+?91[- ]?)?[6-9]\\d{9}\\b"),
        Regex("(?i)\\b(nvapi-|sk-|gh[pousr]_|github_pat_|xox[baprs]-)\\S{12,}")
    )
    fun isSensitive(text: String): Boolean = sensitivePatterns.any { it.containsMatchIn(text) }

    fun relevantFacts(facts: List<String>, query: String, limit: Int = 10): List<String> {
        val safeLimit = limit.coerceIn(0, 30)
        if (safeLimit == 0) return emptyList()
        val ignored = setOf("the", "and", "for", "with", "that", "this", "you", "your", "mujhe", "meri", "mera", "hai", "hain", "pasand", "please")
        val queryTokens = Regex("[a-z0-9]{2,}").findAll(query.lowercase())
            .map { it.value }.filterNot { it in ignored }.toSet()
        val safeFacts = facts.filter { it.isNotBlank() && !isSensitive(it) }
        return safeFacts.withIndex().map { indexed ->
            val tokens = Regex("[a-z0-9]{2,}").findAll(indexed.value.lowercase())
                .map { it.value }.filterNot { it in ignored }.toSet()
            val score = queryTokens.count { it in tokens }
            Triple(score, indexed.index, indexed.value)
        }.sortedWith(compareByDescending<Triple<Int, Int, String>> { it.first }.thenByDescending { it.second })
            .take(safeLimit).map { it.third }
    }
    fun extract(message: String): List<String> {
        val text = message.trim()
        if (text.isBlank() || text.length > MAX_INPUT_CHARS || isSensitive(text)) return emptyList()
        val explicit = listOf(
            Regex("(?i)^(?:please\\s+)?remember\\s+(?:that\\s+)?(.+)$"),
            Regex("(?i)^yaad\\s+rakhna\\s*[:,-]?\\s*(.+)$"),
            Regex("(?i)^yaad\\s+rakhna\\s+ki\\s+(.+)$")
        )
        for (pattern in explicit) {
            val match = pattern.matchEntire(text) ?: continue
            return safeFact(match.groupValues[1])
        }
        val corrections = listOf(
            Regex("(?i)^(?:no|nah|nahi),?\\s*(?:i meant|mera matlab(?: tha)?|what i meant was)\\s+(.+)$"),
            Regex("(?i)^(?:correction|actually),?\\s*(?:i meant|mera matlab(?: hai| tha)?)\\s+(.+)$")
        )
        for (pattern in corrections) {
            val match = pattern.matchEntire(text) ?: continue
            return safeFact(match.groupValues[1])
        }
        val preferenceText = text.replace(Regex("[.!?]+$"), "")
        val preferences = listOf(
            Regex("(?i)^(i prefer .+|i like .+|i dislike .+|i don't like .+|my preference is .+)$"),
            Regex("(?i)^(mujhe .+ pasand hain?|mujhe .+ pasand nahi(?: hai)?|meri preference .+ hai)$")
        )
        if (preferences.any { it.matches(preferenceText) }) return safeFact(preferenceText)
        val names = listOf(
            Regex("(?i)^my name is ([a-z][a-z .'-]{1,48})[.!]?$"),
            Regex("(?i)^mera naam ([a-z][a-z .'-]{1,48}) hai[.!]?$")
        )
        for (pattern in names) {
            val match = pattern.matchEntire(text) ?: continue
            return safeFact("My name is " + match.groupValues[1].trim())
        }
        return emptyList()
    }
    private fun safeFact(value: String): List<String> {
        val fact = value.replace(Regex("\\s+"), " ").trim()
            .trim('"', '\'', '“', '”', '.', '!', '?').take(MAX_FACT_CHARS).trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        if (fact.isBlank() || isSensitive(fact)) return emptyList()
        return listOf(if (fact.endsWith(".")) fact else "$fact.")
    }
}
