package com.hermes.assistant

import org.json.JSONArray
import java.time.Instant

/** Resolves the signed content-package asset from the authenticated private-repository release feed. */
internal object ContentUpdateReleaseLocator {
    private const val EXPECTED_PREFIX = "/riyas433554-dot/vyom-android/releases/download/"
    private const val ASSET_NAME = "vyom-content-update-demo.zip"

    fun latestPackageAssetId(releasesJson: String): Long? {
        val releases = JSONArray(releasesJson)
        val candidates = mutableListOf<Pair<Instant, Long>>()
        for (index in 0 until releases.length()) {
            val release = releases.optJSONObject(index) ?: continue
            if (release.optBoolean("draft", true) || !release.optBoolean("prerelease", false)) continue
            val publishedAt = runCatching { Instant.parse(release.optString("published_at")) }.getOrNull() ?: continue
            val assets = release.optJSONArray("assets") ?: continue
            for (assetIndex in 0 until assets.length()) {
                val asset = assets.optJSONObject(assetIndex) ?: continue
                if (asset.optString("name") != ASSET_NAME) continue
                val assetId = asset.optLong("id", 0L)
                val url = asset.optString("browser_download_url")
                if (assetId > 0L && isTrustedPackageUrl(url)) candidates += publishedAt to assetId
            }
        }
        return candidates.maxByOrNull { it.first }?.second
    }

    fun isTrustedPackageUrl(value: String): Boolean = runCatching {
        val uri = java.net.URI(value)
        uri.scheme == "https" && uri.host == "github.com" &&
            uri.path.startsWith(EXPECTED_PREFIX) && uri.path.endsWith("/$ASSET_NAME")
    }.getOrDefault(false)
}
