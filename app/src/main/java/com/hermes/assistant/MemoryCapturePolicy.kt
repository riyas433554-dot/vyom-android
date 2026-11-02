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
        Regex("(পাসওয়ার্ড|পাসকোড|ওটিপি|এককালীন কোড|যাচাইকরণ কোড|পিন|ইউপিআই পিন|সিভিভি|সিভিসি|ব্যাংক অ্যাকাউন্ট|অ্যাকাউন্ট নম্বর|ক্রেডিট কার্ড|ডেবিট কার্ড|কার্ড নম্বর|আধার|প্যান নম্বর|পাসপোর্ট নম্বর|ওষুধ|প্রেসক্রিপশন|রোগ নির্ণয়|স্বাস্থ্য সমস্যা)"),
        Regex("[০-৯]{13,19}"),
        Regex("(?i)\\b\\d{13,19}\\b"),
        Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE),
        Regex("\\b(?:\\+?91[- ]?)?[6-9]\\d{9}\\b"),
        Regex("(?i)\\b(nvapi-|sk-|gh[pousr]_|github_pat_|xox[baprs]-)\\S{12,}")
    )
    fun isSensitive(text: String): Boolean = sensitivePatterns.any { it.containsMatchIn(text) }

    fun relevantFacts(facts: List<String>, query: String, limit: Int = 10): List<String> {
        val safeLimit = limit.coerceIn(0, 30)
        if (safeLimit == 0) return emptyList()
        val ignored = setOf("the", "and", "for", "with", "that", "this", "you", "your", "mujhe", "meri", "mera", "hai", "hain", "pasand", "please", "আমি", "আমার", "পছন্দ", "করি", "হয়", "হবে", "দাও", "দেন", "এবং", "যে", "আমাকে")
        val queryTokens = semanticTokens(query, ignored)
        val safeFacts = facts.filter { it.isNotBlank() && !isSensitive(it) }
        val scored = safeFacts.withIndex().map { indexed ->
            val tokens = semanticTokens(indexed.value, ignored)
            val score = queryTokens.count { it in tokens }
            Triple(score, indexed.index, indexed.value)
        }
        // Score every fact before applying the limit. Otherwise an older, better lexical
        // match can crowd the newest conflicting preference out of a small result window.
        val conflictScores = scored.mapNotNull { item ->
            conflictTopic(item.third)?.let { topic -> topic to item.first }
        }.groupBy({ it.first }, { it.second })
            .mapValues { (_, scores) -> scores.maxOrNull() ?: 0 }
        return scored.asSequence()
            .filter { item ->
                queryTokens.isEmpty() || item.first > 0 ||
                    (conflictTopic(item.third)?.let { (conflictScores[it] ?: 0) > 0 } == true)
            }
            .sortedWith(
                compareByDescending<Triple<Int, Int, String>> { item ->
                    conflictTopic(item.third)?.let { conflictScores[it] } ?: item.first
                }.thenByDescending { it.second }
            )
            .take(safeLimit)
            .map { it.third }
            .toList()
    }
    fun extract(message: String): List<String> {
        val text = message.trim()
        if (text.isBlank() || text.length > MAX_INPUT_CHARS || isSensitive(text)) return emptyList()
        val explicit = listOf(
            Regex("(?i)^(?:please\\s+)?remember\\s+(?:that\\s+)?(.+)$"),
            Regex("(?i)^yaad\\s+rakhna\\s+ki\\s+(.+)$"),
            Regex("(?i)^yaad\\s+rakhna\\s*[:,-]?\\s*(.+)$"),
            Regex("(?i)^(?:please\\s+)?keep\\s+in\\s+mind\\s+(?:that\\s+)?(.+)$"),
            Regex("^(?:মনে রেখো|মনে রাখবে|মনে রাখবেন)(?: যে)?\\s*[:,,-]?\\s*(.+)$")
        )
        for (pattern in explicit) {
            val match = pattern.matchEntire(text) ?: continue
            return safeFact(match.groupValues[1])
        }
        val corrections = listOf(
            Regex("(?i)^(?:no|nah|nahi),?\\s*(?:i meant|mera matlab(?: tha)?|what i meant was)\\s+(.+)$"),
            Regex("(?i)^(?:correction|actually),?\\s*(?:i meant|mera matlab(?: hai| tha)?)\\s+(.+)$"),
            Regex("^(?:না|সংশোধন),?\\s*(?:আমার মানে ছিল|আমি বলতে চেয়েছিলাম)\\s+(.+)$")
        )
        for (pattern in corrections) {
            val match = pattern.matchEntire(text) ?: continue
            return safeFact(match.groupValues[1])
        }
        val preferenceText = text.replace(Regex("[.!?।]+$"), "")
        val preferences = listOf(
            Regex("(?i)^(i prefer .+|i like .+|i dislike .+|i don't like .+|my preference is .+)$"),
            Regex("(?i)^(mujhe .+ pasand hain?|mujhe .+ pasand nahi(?: hai)?|meri preference .+ hai)$"),
            Regex("^(আমি .+ পছন্দ করি|আমার পছন্দ .+)$")
        )
        if (preferences.any { it.matches(preferenceText) }) return safeFact(preferenceText)
        val englishDirective = Regex("(?i)^(?:going forward|from now on),?\\s*(?:please\\s+)?keep\\s+(?:your\\s+)?replies?\\s+(concise|short|brief|detailed)[.!]?$").matchEntire(text)
        if (englishDirective != null) return safeFact("I prefer ${englishDirective.groupValues[1].lowercase()} replies")
        val hinglishDirective = Regex("(?i)^(?:aage se\\s+)?mujhe\\s+(.+?)\\s+(?:dena|do)[.!]?$").matchEntire(text)
        if (hinglishDirective != null) return safeFact("Mujhe ${hinglishDirective.groupValues[1]} pasand hain")
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

    private fun semanticTokens(text: String, ignored: Set<String>): Set<String> =
        Regex("[\\p{L}\\p{M}\\p{N}]{2,}").findAll(text.lowercase())
            .map { canonicalToken(it.value) }
            .filterNot { it in ignored }
            .toSet()

    /** Small, explicit synonym map; this is lexical retrieval, not embedding/vector search. */
    private fun canonicalToken(token: String): String = when (token) {
        "short", "brief", "concise", "compact", "detailed", "detail", "long", "verbose",
        "thorough", "comprehensive", "length", "verbosity", "depth" -> "response-style"
        "answer", "answers", "reply", "replies", "response", "responses" -> "response"
        "bangla", "বাংলা" -> "bengali"
        "ভাষা", "ভাষায়" -> "language"
        "হিন্দি" -> "hindi"
        "ইংরেজি" -> "english"
        "bullet", "bullets", "list", "lists" -> "list-format"
        "confirm", "confirmation", "approve", "approval", "autonomous", "automatic",
        "automatically", "ask" -> "execution-consent"
        "action", "actions", "task", "tasks", "change", "changes" -> "action"
        else -> token
    }

    private fun conflictTopic(memory: String): String? {
        val value = memory.lowercase()
        if (!Regex("(prefer|like|dislike|preference|pasand|পছন্দ)").containsMatchIn(value)) return null
        if (Regex("(concise|short|brief|detailed|long|verbose|সংক্ষিপ্ত|বিস্তারিত)").containsMatchIn(value) &&
            Regex("(replies?|answers?|responses?|উত্তর)").containsMatchIn(value)) return "response-style"
        if (Regex("(english|hindi|hinglish|bengali|bangla|language|বাংলা|হিন্দি|ইংরেজি)").containsMatchIn(value)) return "language"
        if (Regex("\\b(dark|light)\\s+themes?\\b").containsMatchIn(value)) return "theme"
        if (Regex("(markdown|plain text|bullets?|numbered lists?|prose)").containsMatchIn(value)) return "format"
        if (Regex("(direct|blunt|friendly|formal|casual|polite|warm|critical|supportive)").containsMatchIn(value) &&
            Regex("(answers?|replies?|responses?|communication|tone)").containsMatchIn(value)) return "tone"
        if (Regex("(confirm|confirmation|approval|approve|autonomous|automatic|ask before|without asking)").containsMatchIn(value) &&
            Regex("(actions?|tasks?|changes?)").containsMatchIn(value)) return "execution-consent"
        return null
    }

    private fun safeFact(value: String): List<String> {
        val fact = value.replace(Regex("\\s+"), " ").trim()
            .trim('"', '\'', '“', '”', '.', '!', '?', '।').take(MAX_FACT_CHARS).trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        if (fact.isBlank() || isSensitive(fact)) return emptyList()
        return listOf(if (fact.endsWith(".")) fact else "$fact.")
    }
}
