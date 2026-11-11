package com.hermes.assistant

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.PackageInstaller
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/** Downloads, authenticates and installs a signed Vyom APK as an in-place package update. */
internal class VyomApkUpdateManager(
    private val context: Context,
    private val token: String
) {
    data class PreparedUpdate(
        val apkFile: File,
        val versionName: String,
        val versionCode: Long,
        val commit: String,
        val signerSha256: String
    )

    private val releaseClient = GitHubReleaseAssetClient({ token })
    private val packageManager = context.packageManager

    fun prepareLatest(): PreparedUpdate {
        val release = JSONArray(releaseClient.readReleases(MAX_RELEASES_BYTES)).optJSONObject(0)
            ?: throw IllegalStateException("No published Vyom release was found")
        require(!release.optBoolean("draft", true)) { "Latest Vyom release is still a draft" }
        val releaseTag = release.optString("tag_name")
        require(releaseTag.isNotBlank()) { "Vyom release tag is missing" }
        val assets = release.optJSONArray("assets") ?: throw IllegalStateException("Release assets are missing")
        val manifestAsset = findAsset(assets) { it.optString("name") == UPDATE_MANIFEST_NAME }
            ?: throw IllegalStateException("Signed APK update manifest is missing from the latest release")
        val manifestBytes = releaseClient.readAsset(manifestAsset.optLong("id"), MAX_MANIFEST_BYTES)
        val manifest = JSONObject(String(manifestBytes, Charsets.UTF_8))
        val apkUrl = manifest.optString("url")
        val candidate = UpdateManifestPolicy.Candidate(
            versionCode = manifest.optInt("versionCode", -1),
            sha256 = manifest.optString("sha256"),
            certificateSha256 = manifest.optString("certificateSha256").lowercase(),
            commit = manifest.optString("commit").lowercase(),
            releaseTag = releaseTag,
            apkUrl = apkUrl
        )
        require(UpdateManifestPolicy.isWellFormed(candidate)) {
            "Latest release APK manifest is malformed or points outside its exact release tag"
        }
        require(manifest.optString("apk") == apkUrl) { "APK manifest URL fields disagree" }
        val apkAsset = findAsset(assets) {
            it.optString("browser_download_url") == apkUrl &&
                it.optString("name").matches(Regex("^[A-Za-z0-9._-]+\\.apk$"))
        } ?: throw IllegalStateException("Manifest APK URL does not identify an asset in this release")
        val apkAssetId = apkAsset.optLong("id", 0L)
        require(apkAssetId > 0L) { "APK release asset ID is invalid" }
        val versionName = manifest.optString("versionName", manifest.optString("version"))
        require(versionName.isNotBlank() && versionName.length <= 80) { "APK version name is invalid" }

        val installed = installedPackageInfo()
        val installedVersionCode = packageVersionCode(installed)
        require(candidate.versionCode.toLong() > installedVersionCode) {
            "Latest release is not newer than the installed Vyom version"
        }

        val updateDirectory = File(context.cacheDir, "factory-apk-updates")
        check((updateDirectory.isDirectory || updateDirectory.mkdirs()) && updateDirectory.isDirectory) {
            "Private APK update cache could not be created"
        }
        val apkFile = File.createTempFile("vyom-verified-", ".apk", updateDirectory)
        try {
            releaseClient.downloadAsset(apkAssetId, apkFile, MAX_APK_BYTES)
            val actualSha = sha256(apkFile)
            val archive = archiveIdentity(apkFile, actualSha)
            val accepted = VyomApkUpdatePolicy.isAcceptable(
                candidate = candidate,
                archive = archive,
                expectedPackage = BuildConfig.APPLICATION_ID,
                installedPackage = installed.packageName,
                installedVersionCode = installedVersionCode,
                expectedCertificateSha256 = BuildConfig.REQUIRED_RELEASE_CERT_SHA256.lowercase()
            )
            require(accepted) {
                "APK rejected: package, version, checksum, manifest or fixed signing certificate did not match"
            }
            return PreparedUpdate(
                apkFile = apkFile,
                versionName = versionName,
                versionCode = candidate.versionCode.toLong(),
                commit = candidate.commit,
                signerSha256 = archive.signerSha256
            )
        } catch (error: Exception) {
            apkFile.delete()
            throw error
        }
    }

    fun canRequestInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()

    /** Commits a PackageInstaller full-update session; it never uninstalls or deletes the installed app. */
    fun installInPlace(update: PreparedUpdate): Int {
        require(update.apkFile.isFile && update.apkFile.length() in 1..MAX_APK_BYTES) {
            "Verified APK file is missing or outside the size limit"
        }
        require(canRequestInstall()) {
            "Allow Vyom to install unknown apps in Android settings, then retry the update"
        }
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(BuildConfig.APPLICATION_ID)
            setSize(update.apkFile.length())
        }
        val installer = packageManager.packageInstaller
        val sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)
        try {
            FileInputStream(update.apkFile).use { input ->
                session.openWrite("base.apk", 0L, update.apkFile.length()).use { output ->
                    input.copyTo(output, BUFFER_SIZE)
                    session.fsync(output)
                }
            }
            var pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingFlags = pendingFlags or PendingIntent.FLAG_MUTABLE
            }
            val callbackIntent = Intent(context, VyomApkInstallStatusReceiver::class.java)
                .setAction(VyomApkInstallStatusReceiver.ACTION_INSTALL_STATUS)
            val callback = PendingIntent.getBroadcast(context, sessionId, callbackIntent, pendingFlags)
            session.commit(callback.intentSender)
            return sessionId
        } catch (error: Exception) {
            runCatching { session.abandon() }
            throw error
        } finally {
            session.close()
        }
    }

    @Suppress("DEPRECATION")
    private fun installedPackageInfo(): PackageInfo =
        packageManager.getPackageInfo(BuildConfig.APPLICATION_ID, 0).also {
            require(it.packageName == BuildConfig.APPLICATION_ID) { "Installed package identity mismatch" }
        }

    @Suppress("DEPRECATION")
    private fun archiveIdentity(file: File, digest: String): VyomApkUpdatePolicy.ArchiveIdentity {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val info = packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw IllegalStateException("Downloaded file is not a readable Android APK")
        val signatures: List<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.toList().orEmpty()
        } else {
            info.signatures?.toList().orEmpty()
        }
        require(signatures.isNotEmpty()) { "APK signing certificate is missing" }
        val signerFingerprints = signatures.map { sha256(it.toByteArray()) }.distinct()
        require(signerFingerprints.size == 1) { "APK contains multiple distinct current signers; refusing update" }
        val version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
        return VyomApkUpdatePolicy.ArchiveIdentity(
            packageName = info.packageName,
            versionCode = version,
            sha256 = digest,
            signerSha256 = signerFingerprints.single()
        )
    }

    private fun findAsset(assets: JSONArray, predicate: (JSONObject) -> Boolean): JSONObject? {
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            if (predicate(asset)) return asset
        }
        return null
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val UPDATE_MANIFEST_NAME = "vyom-update.json"
        private const val MAX_RELEASES_BYTES = 512 * 1024
        private const val MAX_MANIFEST_BYTES = 64 * 1024
        private const val MAX_APK_BYTES = 150L * 1024L * 1024L
        private const val BUFFER_SIZE = 16 * 1024
    }
}
