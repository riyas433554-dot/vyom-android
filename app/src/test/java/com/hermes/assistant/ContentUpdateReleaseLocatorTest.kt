package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject

class ContentUpdateReleaseLocatorTest {
    @Test fun selectsNewestSignedContentPackageFromPublicRepositoryReleases() {
        val releases = JSONArray()
            .put(JSONObject().put("draft", false).put("prerelease", true).put("published_at", "2026-11-02T10:00:00Z")
                .put("assets", JSONArray().put(JSONObject().put("name", "vyom-content-update-demo.zip")
                    .put("browser_download_url", "https://github.com/riyas433554-dot/vyom-android/releases/download/old/vyom-content-update-demo.zip"))))
            .put(JSONObject().put("draft", false).put("prerelease", true).put("published_at", "2026-11-03T10:00:00Z")
                .put("assets", JSONArray().put(JSONObject().put("name", "vyom-content-update-demo.zip")
                    .put("browser_download_url", "https://github.com/riyas433554-dot/vyom-android/releases/download/new/vyom-content-update-demo.zip"))))
        assertEquals(
            "https://github.com/riyas433554-dot/vyom-android/releases/download/new/vyom-content-update-demo.zip",
            ContentUpdateReleaseLocator.latestPackageUrl(releases.toString())
        )
    }

    @Test fun ignoresDraftAndReleasesWithoutContentPackage() {
        val releases = JSONArray()
            .put(JSONObject().put("draft", true).put("prerelease", true).put("published_at", "2026-11-04T10:00:00Z")
                .put("assets", JSONArray().put(JSONObject().put("name", "vyom-content-update-demo.zip")
                    .put("browser_download_url", "https://github.com/riyas433554-dot/vyom-android/releases/download/draft/vyom-content-update-demo.zip"))))
            .put(JSONObject().put("draft", false).put("prerelease", true).put("published_at", "2026-11-03T10:00:00Z")
                .put("assets", JSONArray().put(JSONObject().put("name", "build-info.txt")
                    .put("browser_download_url", "https://github.com/riyas433554-dot/vyom-android/releases/download/old/build-info.txt"))))
        assertNull(ContentUpdateReleaseLocator.latestPackageUrl(releases.toString()))
    }

    @Test fun rejectsContentPackageUrlOutsideTheExactRepositoryReleasePath() {
        val releases = JSONArray().put(JSONObject().put("draft", false).put("prerelease", true)
            .put("published_at", "2026-11-03T10:00:00Z")
            .put("assets", JSONArray().put(JSONObject().put("name", "vyom-content-update-demo.zip")
                .put("browser_download_url", "https://attacker.example/vyom-content-update-demo.zip"))))
        assertNull(ContentUpdateReleaseLocator.latestPackageUrl(releases.toString()))
    }
}