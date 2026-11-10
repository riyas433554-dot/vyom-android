package com.hermes.assistant

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject

class ContentUpdateReleaseLocatorTest {
    private fun asset(id: Long, tag: String, name: String = "vyom-content-update-demo.zip") =
        JSONObject().put("id", id).put("name", name)
            .put("browser_download_url", "https://github.com/riyas433554-dot/vyom-android/releases/download/$tag/$name")

    @Test fun selectsPackageFromTheSingleNewestRelease() {
        val releases = JSONArray().put(JSONObject().put("draft", false).put("prerelease", false)
            .put("assets", JSONArray().put(asset(200L, "new"))))
        assertEquals(200L, ContentUpdateReleaseLocator.latestPackageAssetId(releases.toString()))
    }

    @Test fun ignoresDraftAndNewestReleaseWithoutContentPackage() {
        val draft = JSONArray().put(JSONObject().put("draft", true).put("assets", JSONArray().put(asset(300L, "draft"))))
        assertNull(ContentUpdateReleaseLocator.latestPackageAssetId(draft.toString()))
        val noPackage = JSONArray().put(JSONObject().put("draft", false)
            .put("assets", JSONArray().put(asset(400L, "new", "build-info.txt"))))
        assertNull(ContentUpdateReleaseLocator.latestPackageAssetId(noPackage.toString()))
    }

    @Test fun rejectsUntrustedContentPackageUrl() {
        val releases = JSONArray().put(JSONObject().put("draft", false)
            .put("assets", JSONArray().put(JSONObject().put("id", 500L).put("name", "vyom-content-update-demo.zip")
                .put("browser_download_url", "https://attacker.example/vyom-content-update-demo.zip"))))
        assertNull(ContentUpdateReleaseLocator.latestPackageAssetId(releases.toString()))
    }

    @Test fun neverFallsBackToOlderReleaseWhenNewestHasNoPackage() {
        val releases = JSONArray().put(JSONObject().put("draft", false).put("assets", JSONArray()))
            .put(JSONObject().put("draft", false).put("assets", JSONArray().put(asset(600L, "older"))))
        assertNull(ContentUpdateReleaseLocator.latestPackageAssetId(releases.toString()))
    }
}
