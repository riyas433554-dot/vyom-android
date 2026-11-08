package com.hermes.assistant

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.ByteArrayInputStream
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64

/** Downloads, verifies, stages and applies signed content packages from the same GitHub release feed. */
internal class ContentUpdateManager(private val context: Context) {
    private val store = ContentUpdateSlotStore(File(context.filesDir, "content-slots"))
    private val uiStore by lazy { UiBundleStore(context) }
    private val githubReleaseAssets by lazy {
        GitHubReleaseAssetClient(tokenProvider = { GitHubReleaseTokenStore.read(context) })
    }

    fun activeVersion(): String = store.activeVersion()

    fun ensureSeeded() {
        val manifest = context.assets.open("ui-bundle/manifest.json")
            .bufferedReader(Charsets.UTF_8).use { org.json.JSONObject(it.readText()) }
        val version = manifest.optString("version").ifBlank { "1.0.0" }
        val files = UiBundleIntegrity.allowedAssetNames().associate { name ->
            "ui-bundle/$name" to context.assets.open("ui-bundle/$name").use { it.readBytes() }
        }
        store.seedInitialSlot(version, files)
        uiStore.seedBundledVersion()
        val active = store.activeVersion()
        if (active.isNotBlank() && uiStore.activeVersion() != active) syncUiBundleToUiHost(active)
    }

    fun applyLatestRelease(): ContentUpdateResult {
        ensureSeeded()
        return try {
            val releases = githubReleaseAssets.readReleases(2 * 1024 * 1024)
            val assetId = ContentUpdateReleaseLocator.latestPackageAssetId(releases)
                ?: return ContentUpdateResult(activeVersion(), false, emptyList(), null,
                    "No signed no-APK content package is available in the release feed")
            val parsed = ContentUpdatePackageArchive.read(githubReleaseAssets.readAsset(assetId, MAX_PACKAGE_BYTES))
            applySignedPackage(
                parsed.manifest, parsed.detachedSignature, parsed.files,
                ContentUpdatePackageVerifier(installedSigningPublicKey())
            )
        } catch (error: Exception) {
            ContentUpdateResult(activeVersion(), false, emptyList(), null,
                "Content update rejected; active slot unchanged: ${error.message ?: "verification/download error"}")
        }
    }

    /** Creates and signs a content-only package locally; it never mutates the active slot directly. */
    fun applyLocalUiFiles(files: Map<String, ByteArray>, ownerPrompt: String): ContentUpdateResult {
        ensureSeeded()
        require(files.keys == UiBundleIntegrity.allowedAssetNames()) {
            "A local UI update must contain exactly index.html, styles.css and app.js"
        }
        require(ownerPrompt.isNotBlank() && ownerPrompt.length <= 1_500) { "Owner prompt is empty or too long" }
        val html = String(files["index.html"] ?: error("Updated index.html is missing"), Charsets.UTF_8)
        val heading = Regex("(?is)<h1[^>]*>(.*?)</h1>").find(html)?.groupValues?.get(1)
            ?.replace(Regex("<[^>]+>"), "")?.replace("&amp;", "&")?.trim().orEmpty()
        require(heading.isNotBlank()) { "Updated UI must contain a visible h1 heading for the presence probe" }
        val version = nextContentVersion()
        val packageFiles = files.mapKeys { (name, _) -> "ui-bundle/$name" }
        val manifest = ContentUpdateManifest(
            version = version,
            minAppVersion = BuildConfig.VERSION_NAME,
            changes = listOf(ownerPrompt.take(500)),
            files = packageFiles.map { (path, bytes) -> ContentUpdateFile(path, UiBundleIntegrity.sha256(bytes)) },
            probes = listOf(
                ContentUpdateProbe("presence", ContentProbeKind.PRESENCE, "ui-bundle/index.html", heading),
                ContentUpdateProbe("performance", ContentProbeKind.PERFORMANCE, "ui-bundle/index.html",
                    baselineKey = "update-center-page-load")
            ),
            rollbackTo = "previous"
        )
        val signer = ContentUpdateLocalSigner(context)
        val signature = signer.sign(manifest.canonicalPayload())
        return applySignedPackage(
            manifest, signature, packageFiles,
            ContentUpdatePackageVerifier(signer.publicKeyBase64())
        )
    }

    private fun applySignedPackage(
        manifest: ContentUpdateManifest,
        signature: ByteArray,
        files: Map<String, ByteArray>,
        verifier: ContentUpdatePackageVerifier
    ): ContentUpdateResult {
        manifest.probes.filter { it.kind == ContentProbeKind.PERFORMANCE }.forEach { probe ->
            if (!store.hasPerformanceBaseline(probe.baselineKey)) {
                val oldFile = runCatching { store.activeFile(probe.target) }.getOrNull()
                if (oldFile != null) store.recordPerformanceBaseline(probe.baselineKey, measureRead(oldFile))
            }
        }
        val result = store.applySigned(
            manifest, signature, files, BuildConfig.VERSION_NAME, verifier,
            ContentUpdateProbeRunner { probe, stagedSlot ->
                val started = System.nanoTime()
                val file = File(stagedSlot, probe.target)
                val bytes = if (file.isFile) file.readBytes() else ByteArray(0)
                val duration = ((System.nanoTime() - started) / 1_000_000L).coerceAtLeast(0L)
                val passed = when (probe.kind) {
                    ContentProbeKind.PRESENCE -> bytes.isNotEmpty() &&
                        String(bytes, Charsets.UTF_8).contains(probe.expected)
                    ContentProbeKind.PERFORMANCE -> bytes.isNotEmpty()
                }
                ContentProbeResult(probe.id, passed, duration,
                    if (passed) "Verified staged content ${probe.target}" else "Expected content not found in ${probe.target}")
            }
        )
        if (result.passed) {
            try {
                syncUiBundleToUiHost(manifest.version)
                val expectedPresence = manifest.probes.firstOrNull { it.kind == ContentProbeKind.PRESENCE }?.expected.orEmpty()
                context.getSharedPreferences(CONTENT_PREFS, Context.MODE_PRIVATE).edit()
                    .putString("expected_${manifest.version}", expectedPresence).apply()
            } catch (error: Exception) {
                store.rollback()
                throw IllegalStateException("Content slot activated but UI host rejected it; previous slot restored", error)
            }
        }
        return result
    }

    private fun nextContentVersion(): String {
        val parts = activeVersion().split('.')
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toLongOrNull() ?: 0L
        val nextPatch = maxOf(patch + 1L, System.currentTimeMillis())
        return "$major.$minor.$nextPatch"
    }

    fun rollback(): Boolean {
        if (!store.rollback()) return false
        syncUiBundleToUiHost(activeVersion())
        return true
    }

    fun rollbackAfterPageLoadFailure(failedVersion: String): Boolean {
        if (!store.rollbackAfterLoadFailure(failedVersion)) return false
        syncUiBundleToUiHost(activeVersion())
        return true
    }

    fun history(): List<String> = store.history()
    fun activePresenceExpectation(): String {
        val version = activeVersion()
        if (version.isBlank()) return ""
        return context.getSharedPreferences(CONTENT_PREFS, Context.MODE_PRIVATE)
            .getString("expected_$version", "").orEmpty()
    }

    /** A second uncaught process crash in 60 seconds restores the previous content slot. */
    fun installCrashRollbackHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is ContentUpdateCrashHandler) return
        Thread.setDefaultUncaughtExceptionHandler(ContentUpdateCrashHandler(this, previous))
    }

    fun recordUncaughtCrash(nowMs: Long = System.currentTimeMillis()) {
        runCatching { store.recordCrashAndRollback(nowMs) }
    }

    fun markStableLaunch() {
        runCatching { store.recordStableLaunch() }
    }

    private fun syncUiBundleToUiHost(version: String) {
        val names = UiBundleIntegrity.allowedAssetNames()
        val files = names.associateWith { name -> store.activeFile("ui-bundle/$name").readBytes() }
        val hashes = files.mapValues { (_, bytes) -> UiBundleIntegrity.sha256(bytes) }
        uiStore.installBundle(version, files, hashes)
    }

    private fun installedSigningPublicKey(): String {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
            else PackageManager.GET_SIGNATURES
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, flags)
        val certificateBytes = if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
        } else {
            info.signatures?.firstOrNull()?.toByteArray()
        } ?: throw IllegalStateException("Installed app signing certificate is unavailable")
        val certificate = CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(certificateBytes)) as X509Certificate
        return Base64.getEncoder().encodeToString(certificate.publicKey.encoded)
    }

    private fun measureRead(file: File): Long {
        val started = System.nanoTime()
        file.readBytes()
        return ((System.nanoTime() - started) / 1_000_000L).coerceAtLeast(0L)
    }

    private class ContentUpdateCrashHandler(
        private val manager: ContentUpdateManager,
        private val delegate: Thread.UncaughtExceptionHandler?
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, error: Throwable) {
            manager.recordUncaughtCrash()
            delegate?.uncaughtException(thread, error)
        }
    }

    companion object {
        private const val MAX_PACKAGE_BYTES = 10 * 1024 * 1024
        private const val CONTENT_PREFS = "vyom_content_update"
    }
}