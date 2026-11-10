package com.hermes.assistant

import org.json.JSONArray

/** Resolves the signed content package from the single newest release returned by per_page=1. */
internal object ContentUpdateReleaseLocator {
    private const val EXPECTED_PREFIX = "/riyas433554-dot/vyom-android/releases/download/"
    private const val ASSET_NAME = "vyom-content-update-demo.zip"

    fun latestPackageAssetId(releasesJson: String): Long? {
        val release = JSONArray(releasesJson).optJSONObject(0) ?: return null
        if (release.optBoolean("draft", true)) return null
        val assets = release.optJSONArray("assets") ?: return null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            if (asset.optString("name") != ASSET_NAME) continue
            val id = asset.optLong("id", 0L)
            if (id > 0L && isTrustedPackageUrl(asset.optString("browser_download_url"))) return id
        }
        return null
    }

    fun isTrustedPackageUrl(value: String): Boolean = runCatching {
        val uri = java.net.URI(value)
        uri.scheme == "https" && uri.host == "github.com" &&
            uri.path.startsWith(EXPECTED_PREFIX) && uri.path.endsWith("/$ASSET_NAME") &&
            uri.userInfo == null && uri.rawQuery == null && uri.rawFragment == null
    }.getOrDefault(false)
}
