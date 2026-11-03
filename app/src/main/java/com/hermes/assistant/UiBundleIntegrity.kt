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
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.US_ASCII),
            expectedSha256.lowercase().toByteArray(Charsets.US_ASCII)
        )
    }

    fun allowedAssetNames(): Set<String> = allowedNames
}
