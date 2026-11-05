package com.hermes.assistant

import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI

/** Authenticated access to private GitHub release metadata and assets. Redirects never receive the PAT. */
internal class GitHubReleaseAssetClient(
    private val tokenProvider: () -> String,
    private val connectionFactory: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) {
    fun readReleases(maxBytes: Int): String {
        val connection = connectionFactory(URL(RELEASES_API)).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = false
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Vyom-Android")
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", GitHubReleaseAssetPolicy.authorizationHeader(tokenProvider()))
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException(GitHubReleaseAssetPolicy.httpErrorMessage(
                    code, readErrorBody(connection), connection.getHeaderField("X-GitHub-Request-Id")
                ))
            }
            return String(readBounded(connection, maxBytes), Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    fun readAsset(assetId: Long, maxBytes: Int): ByteArray {
        val connection = openAsset(assetId)
        try {
            return readBounded(connection, maxBytes)
        } finally {
            connection.disconnect()
        }
    }

    fun downloadAsset(assetId: Long, target: File, maxBytes: Long) {
        val connection = openAsset(assetId)
        try {
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= maxBytes) { "GitHub release asset exceeds size limit" }
                        output.write(buffer, 0, count)
                    }
                }
            }
            require(target.isFile && target.length() > 0L) { "GitHub release asset is empty" }
        } catch (error: Exception) {
            target.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun openAsset(assetId: Long): HttpURLConnection {
        val apiConnection = connectionFactory(URL(GitHubReleaseAssetPolicy.assetApiUrl(assetId))).apply {
            connectTimeout = 15_000
            readTimeout = 120_000
            instanceFollowRedirects = false
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Vyom-Android")
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("Authorization", GitHubReleaseAssetPolicy.authorizationHeader(tokenProvider()))
        }
        val code = try { apiConnection.responseCode } catch (error: Exception) {
            apiConnection.disconnect()
            throw error
        }
        if (code in 200..299) return apiConnection
        if (code !in REDIRECT_CODES) {
            val diagnostic = GitHubReleaseAssetPolicy.httpErrorMessage(
                code, readErrorBody(apiConnection), apiConnection.getHeaderField("X-GitHub-Request-Id"),
                prefix = "GitHub release asset"
            )
            apiConnection.disconnect()
            throw IllegalStateException(diagnostic)
        }
        val location = apiConnection.getHeaderField("Location")
        apiConnection.disconnect()
        require(GitHubReleaseAssetPolicy.isTrustedAssetRedirect(location.orEmpty())) {
            "GitHub release asset returned an untrusted redirect"
        }
        // The signed redirect URL is sufficient. Never forward the GitHub PAT to its host.
        val redirected = connectionFactory(URL(location)).apply {
            connectTimeout = 15_000
            readTimeout = 120_000
            instanceFollowRedirects = false
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Vyom-Android")
            setRequestProperty("Accept", "application/octet-stream")
        }
        val redirectedCode = try { redirected.responseCode } catch (error: Exception) {
            redirected.disconnect()
            throw error
        }
        if (redirectedCode !in 200..299) {
            val diagnostic = GitHubReleaseAssetPolicy.httpErrorMessage(
                redirectedCode, readErrorBody(redirected), redirected.getHeaderField("X-GitHub-Request-Id"),
                prefix = "GitHub release asset download"
            )
            redirected.disconnect()
            throw IllegalStateException(diagnostic)
        }
        return redirected
    }

    private fun readErrorBody(connection: HttpURLConnection): String {
        val stream = connection.errorStream ?: return ""
        return runCatching {
            stream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var total = 0
                while (total < MAX_ERROR_BODY_BYTES) {
                    val count = input.read(buffer, 0, minOf(buffer.size, MAX_ERROR_BODY_BYTES - total))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    total += count
                }
                String(output.toByteArray(), Charsets.UTF_8)
            }
        }.getOrDefault("")
    }

    private fun readBounded(connection: HttpURLConnection, maxBytes: Int): ByteArray {
        require(maxBytes > 0) { "Response limit must be positive" }
        val output = ByteArrayOutputStream()
        connection.inputStream.use { input ->
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= maxBytes) { "GitHub response exceeds size limit" }
                output.write(buffer, 0, count)
            }
        }
        return output.toByteArray()
    }

    companion object {
        private const val RELEASES_API =
            "https://api.github.com/repos/riyas433554-dot/vyom-android/releases?per_page=30"
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        private const val MAX_ERROR_BODY_BYTES = 8 * 1024
    }
}

internal object GitHubReleaseAssetPolicy {
    private const val API_PREFIX =
        "https://api.github.com/repos/riyas433554-dot/vyom-android/releases/assets/"

    fun assetApiUrl(assetId: Long): String {
        require(assetId > 0L) { "Invalid GitHub release asset ID" }
        return API_PREFIX + assetId
    }

    fun isTrustedAssetApiUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme == "https" && uri.host == "api.github.com" &&
            uri.path.matches(Regex("/repos/riyas433554-dot/vyom-android/releases/assets/[1-9][0-9]*")) &&
            uri.rawQuery == null && uri.rawFragment == null
    }.getOrDefault(false)

    fun authorizationHeader(token: String): String {
        val clean = token.trim()
        require(clean.isNotEmpty() && clean.length <= 500 &&
            clean.none { it == '\r' || it == '\n' }) { "GitHub token is missing or invalid; save it in Settings" }
        return "Bearer $clean"
    }

    fun httpErrorMessage(
        status: Int,
        responseBody: String,
        requestId: String?,
        prefix: String = "GitHub release API"
    ): String {
        val rawMessage = Regex("\"message\"\\s*:\\s*\"([^\"]{0,500})\"")
            .find(responseBody.take(8 * 1024))?.groupValues?.getOrNull(1).orEmpty()
        val message = rawMessage
            .replace("\\n", " ")
            .replace("\\r", " ")
            .replace("\\t", " ")
            .filterNot { it.isISOControl() }
            .trim()
            .take(240)
        val safeRequestId = requestId?.takeIf {
            it.length in 1..80 && it.all { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '_' }
        }
        return buildString {
            append("$prefix HTTP $status")
            if (message.isNotBlank()) append(": $message")
            if (safeRequestId != null) append(" (request ID $safeRequestId)")
        }
    }

    fun isTrustedAssetRedirect(value: String): Boolean = runCatching {
        val uri = URI(value)
        if (uri.scheme != "https" || uri.userInfo != null || uri.rawFragment != null) return@runCatching false
        when (uri.host?.lowercase()) {
            "release-assets.githubusercontent.com", "objects.githubusercontent.com" -> true
            "github.com" -> uri.path.startsWith("/riyas433554-dot/vyom-android/releases/download/")
            else -> false
        }
    }.getOrDefault(false)
}
