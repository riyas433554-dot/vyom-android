package com.hermes.assistant

import java.security.MessageDigest

internal object UiBundleIntegrity {
    private val allowedNames = setOf("index.html", "styles.css", "app.js")

    fun isAllowedAssetName(name: String): Boolean = name in allowedNames

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    fun matches(name: String, bytes: ByteArray, expectedSha256: String): Boolean {
        if (!isAllowedAssetName(name) || !expectedSha256.matches(Regex("(?i)^[a-f0-9]{64}$"))) return false
        val actual = sha256(bytes)
        val matchesHash = MessageDigest.isEqual(
            actual.toByteArray(Charsets.US_ASCII),
            expectedSha256.lowercase().toByteArray(Charsets.US_ASCII)
        )
        return matchesHash && isSafeContent(name, bytes)
    }

    fun isSafeContent(name: String, bytes: ByteArray): Boolean {
        if (!isAllowedAssetName(name)) return false
        val content = String(bytes, Charsets.UTF_8)
        return when (name) {
            "index.html" -> {
                val lower = content.lowercase()
                lower.contains("content-security-policy") &&
                    lower.contains("default-src 'self'") &&
                    lower.contains("script-src 'self'") &&
                    !Regex("(?i)<(iframe|object|embed)\\b").containsMatchIn(content) &&
                    !Regex("(?i)(src|href)\\s*=\\s*['\\\"]https?://").containsMatchIn(content) &&
                    !Regex("(?i)\\son[a-z]+\\s*=").containsMatchIn(content) &&
                    !content.contains("javascript:", ignoreCase = true) &&
                    !content.contains("data:text/html", ignoreCase = true)
            }
            "styles.css" -> !Regex("(?i)@import\\s+url\\s*\\(\\s*['\\\"]?https?://").containsMatchIn(content) &&
                !Regex("(?i)url\\s*\\(\\s*['\\\"]?https?://").containsMatchIn(content) &&
                !content.contains("expression(", ignoreCase = true) &&
                !content.contains("-moz-binding", ignoreCase = true)
            "app.js" -> {
                val lower = content.lowercase()
                val forbidden = listOf(
                    "eval(", "new function(", "fetch(", "xmlhttprequest",
                    "websocket", "import(", "sendbeacon(", "document.cookie",
                    "localstorage", "sessionstorage", "indexeddb", "window.open(",
                    "location.href", "location.assign(", "window.location", "document.location",
                    "document.write(", "javascript:"
                )
                forbidden.none { it in lower } && !content.contains("Function(")
            }
            else -> false
        }
    }

    fun allowedAssetNames(): Set<String> = allowedNames
}
