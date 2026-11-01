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
        val queryTokens = Regex("[\\p{L}\\p{M}\\p{N}]{2,}").findAll(query.lowercase())
            .map { it.value }.filterNot { it in ignored }.toSet()
        val safeFacts = facts.filter { it.isNotBlank() && !isSensitive(it) }
        return safeFacts.withIndex().map { indexed ->
            val tokens = Regex("[\\p{L}\\p{M}\\p{N}]{2,}").findAll(indexed.value.lowercase())
                .map { it.value }.filterNot { it in ignored }.toSet()
            val score = queryTokens.count { it in tokens }
            Triple(score, indexed.index, indexed.value)
        }.sortedWith(compareByDescending<Triple<Int, Int, String>> { it.first }.thenByDescending { it.second })
            .filter { queryTokens.isEmpty() || it.first > 0 }
            .take(safeLimit).toMutableList().also { selected ->
                val conflictGroups = selected.indices.groupBy { conflictTopic(selected[it].third) }
                    .filterKeys { it != null }.values
                conflictGroups.forEach { positions ->
                    if (positions.size > 1) {
                        val newestFirst = positions.map { selected[it] }.sortedByDescending { it.second }
                        positions.forEachIndexed { index, position -> selected[position] = newestFirst[index] }
                    }
                }
            }.map { it.third }
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

    private fun conflictTopic(memory: String): String? {
        val value = memory.lowercase()
        if (!Regex("(prefer|like|dislike|preference|pasand|পছন্দ)").containsMatchIn(value)) return null
        if (Regex("(concise|short|brief|detailed|long|verbose|সংক্ষিপ্ত|বিস্তারিত)").containsMatchIn(value) &&
            Regex("(replies?|answers?|responses?|উত্তর)").containsMatchIn(value)) return "response-style"
        if (Regex("(english|hindi|hinglish|bengali|bangla|language|বাংলা|হিন্দি|ইংরেজি)").containsMatchIn(value)) return "language"
        if (Regex("\\b(dark|light)\\s+themes?\\b").containsMatchIn(value)) return "theme"
        if (Regex("(markdown|plain text|bullets?|numbered lists?|prose)").containsMatchIn(value)) return "format"
        return null
    }

    private fun safeFact(value: String): List<String> {
        val fact = value.replace(Regex("\\s+"), " ").trim()
            .trim('"', '\'', '“', '”', '.', '!', '?', '।').take(MAX_FACT_CHARS).trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        if (fact.isBlank() || isSensitive(fact)) return emptyList()
        return listOf(if (fact.endsWith(".")) fact else "$fact.")
    }
}
