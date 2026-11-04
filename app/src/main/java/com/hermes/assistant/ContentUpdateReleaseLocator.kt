package com.hermes.assistant

import org.json.JSONArray
import java.net.URI
import java.time.Instant

/** Resolves only the signed content-package asset from this repository's published release feed. */
internal object ContentUpdateReleaseLocator {
    private const val EXPECTED_HOST = "github.com"
    private const val EXPECTED_PREFIX = "/riyas433554-dot/vyom-android/releases/download/"
    private const val ASSET_NAME = "vyom-content-update-demo.zip"

    fun latestPackageUrl(releasesJson: String): String? {
        val releases = JSONArray(releasesJson)
        val candidates = mutableListOf<Pair<Instant, String>>()
        for (index in 0 until releases.length()) {
            val release = releases.optJSONObject(index) ?: continue
            if (release.optBoolean("draft", true) || !release.optBoolean("prerelease", false)) continue
            val publishedAt = runCatching { Instant.parse(release.optString("published_at")) }.getOrNull() ?: continue
            val assets = release.optJSONArray("assets") ?: continue
            for (assetIndex in 0 until assets.length()) {
                val asset = assets.optJSONObject(assetIndex) ?: continue
                if (asset.optString("name") != ASSET_NAME) continue
                val url = asset.optString("browser_download_url")
                if (isTrustedPackageUrl(url)) candidates += publishedAt to url
            }
        }
        return candidates.maxByOrNull { it.first }?.second
    }

    fun isTrustedPackageUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme.equals("https", true) &&
            uri.host.equals(EXPECTED_HOST, true) &&
            uri.userInfo == null &&
            uri.port == -1 &&
            uri.path.startsWith(EXPECTED_PREFIX) &&
            uri.path.endsWith("/$ASSET_NAME") &&
            uri.query == null && uri.fragment == null
    }.getOrDefault(false)
}
