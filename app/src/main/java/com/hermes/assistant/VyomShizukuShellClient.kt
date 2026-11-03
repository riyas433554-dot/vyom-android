package com.hermes.assistant

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.content.pm.PackageManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import rikka.shizuku.Shizuku

/** Shizuku client. No permission prompt is shown until the owner explicitly taps Authorize. */
internal class VyomShizukuShellClient(
    context: Context,
    private val onStatus: (String) -> Unit
) : ServiceConnection {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val activeRequests = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var remote: IVyomShellService? = null
    @Volatile private var bound = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refreshStatus() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        invalidateBoundService()
        emit("Shizuku disconnected. Active shell requests are no longer available. Restart it from its app, then explicitly reconnect.")
    }
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                emit("Shizuku permission granted by owner. Binding the allowlisted shell service.")
                bindUserService()
            } else {
                invalidateBoundService()
                emit("Shizuku permission denied or revoked. Bound service and active requests were invalidated; no shell command can run.")
            }
        }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        refreshStatus()
    }

    fun refreshStatus() {
        val installed = try {
            appContext.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: Exception) { false }
        if (!installed) {
            invalidateBoundService()
            emit("Shizuku is not installed.")
            return
        }
        val alive = try { Shizuku.pingBinder() } catch (_: Exception) { false }
        if (!alive) {
            invalidateBoundService()
            emit("Shizuku is installed but not running. Start it yourself using Wireless debugging.")
            return
        }
        val version = try { Shizuku.getVersion() } catch (_: Exception) { 0 }
        val uid = try { Shizuku.getUid() } catch (_: Exception) { -1 }
        if (version < 11) {
            invalidateBoundService()
            emit("Shizuku API older than v11 is unsupported.")
            return
        }
        val identity = if (uid == 2000) "ADB shell" else if (uid == 0) "root" else "unknown identity"
        if (uid != 2000) {
            invalidateBoundService()
            emit("Shizuku identity is $identity (UID $uid). Vyom Power Mode requires the non-root ADB shell identity (UID 2000); Level 2 and UserService binding are blocked.")
            return
        }
        val authorized = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) { false }
        if (!authorized) {
            invalidateBoundService()
            emit("Shizuku binder connected (UID $uid; $identity), but Vyom is NOT authorized. LEVEL 2 is unavailable; tap AUTHORIZE VYOM IN SHIZUKU and approve Android's prompt.")
            return
        }
        emit("Shizuku binder connected (UID $uid; $identity). Owner authorization is granted. Read-back commands remain limited to the allowlist; no arbitrary shell input is accepted.")
    }

    fun requestOwnerAuthorization(activity: Activity) {
        val alive = try { Shizuku.pingBinder() } catch (_: Exception) { false }
        if (!alive) {
            emit("Start Shizuku first. Vyom cannot start or pair Shizuku for you.")
            return
        }
        try {
            val uid = Shizuku.getUid()
            if (uid != 2000) {
                emit("Shizuku UserService requires ADB shell UID 2000. Current UID is $uid; authorization request and binding were not attempted.")
                return
            }
            if (Shizuku.isPreV11()) {
                emit("Shizuku API older than v11 is unsupported.")
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                emit("Shizuku permission is already granted. Binding service.")
                bindUserService()
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                emit("Shizuku permission was previously denied. Open Shizuku and review Vyom's authorization.")
            } else {
                Shizuku.requestPermission(REQUEST_CODE)
            }
        } catch (e: Exception) {
            emit("Could not request Shizuku authorization: ${e.javaClass.simpleName}")
        }
    }

    private fun bindUserService() {
        // Re-read all three trust inputs at the binding boundary; earlier authorization
        // checks are not reusable because binder/permission state can change asynchronously.
        val binderAlive = try { Shizuku.pingBinder() } catch (_: Exception) { false }
        val permissionGranted = if (binderAlive) {
            try { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
            catch (_: Exception) { false }
        } else false
        val shizukuUid = if (binderAlive) {
            try { Shizuku.getUid() } catch (_: Exception) { -1 }
        } else -1
        if (!PowerModePolicy.mayBindShizukuUserService(binderAlive, permissionGranted, shizukuUid)) {
            invalidateBoundService()
            emit("Shizuku bind blocked: live binder, owner authorization and ADB shell UID 2000 are all required.")
            return
        }
        if (bound && remote != null) {
            emit("Allowlisted Shizuku shell service is connected.")
            return
        }
        try {
            val args = Shizuku.UserServiceArgs(
                ComponentName(appContext.packageName, VyomShizukuUserService::class.java.name)
            ).daemon(false)
                .processNameSuffix("vyom_shell")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE)
            Shizuku.bindUserService(args, this)
            bound = true
            emit("Binding the Shizuku UserService. Waiting for binder.")
        } catch (e: Exception) {
            bound = false
            emit("Could not bind Shizuku service: ${e.javaClass.simpleName}")
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        remote = if (service?.pingBinder() == true) IVyomShellService.Stub.asInterface(service) else null
        bound = remote != null
        emit(if (remote != null) "Shizuku shell service connected. Only allowlisted commands are available." else "Invalid Shizuku service binder.")
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        invalidateBoundService()
        emit("Shizuku shell service disconnected. Pending requests must be retried after an explicit reconnect.")
    }

    fun capabilityLevel(accessibilityEnabled: Boolean): PowerModePolicy.CapabilityLevel {
        val running = try { Shizuku.pingBinder() } catch (_: Exception) { false }
        val authorized = if (running) {
            try { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
            catch (_: Exception) { false }
        } else false
        val uid = if (running) try { Shizuku.getUid() } catch (_: Exception) { -1 } else -1
        val service = remote
        val remoteAlive = try { service?.asBinder()?.isBinderAlive == true } catch (_: Exception) { false }
        val serviceAuthorized = PowerModePolicy.mayUseBoundShizukuUserService(
            running, authorized, uid, remoteAlive, bound
        )
        if (PowerModePolicy.mustInvalidateShizukuUserService(
                running, authorized, uid, remoteAlive, bound
            )) invalidateBoundService()
        return PowerModePolicy.capabilityLevel(
            shizukuRunning = running,
            shizukuAuthorized = authorized,
            shizukuUid = uid,
            accessibilityEnabled = accessibilityEnabled,
            userServiceBound = serviceAuthorized,
            remoteBinderAlive = remoteAlive
        )
    }

    /** Clear every local handle when owner authorization or either binder is no longer trustworthy. */
    private fun invalidateBoundService() {
        val previousRemote = remote
        val remoteBinderAlive = try {
            previousRemote?.asBinder()?.isBinderAlive == true
        } catch (_: Exception) { false }
        if (previousRemote != null) {
            // Permission loss must not silently orphan work already dispatched to
            // the shell process. Cancellation is best-effort and never delays
            // clearing the now-untrusted local capability state.
            ShizukuRequestCancellation.cancelOutstanding(
                remoteBinderAlive = remoteBinderAlive,
                requestIds = activeRequests.toList()
            ) { requestId -> previousRemote.cancel(requestId) }
        }
        remote = null
        bound = false
        activeRequests.clear()
    }

    private fun hasLiveServiceAuthorization(service: IVyomShellService): Boolean {
        val binderAlive = try { Shizuku.pingBinder() } catch (_: Exception) { false }
        val permissionGranted = if (binderAlive) {
            try { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
            catch (_: Exception) { false }
        } else false
        val uid = if (binderAlive) {
            try { Shizuku.getUid() } catch (_: Exception) { -1 }
        } else -1
        val remoteBinderAlive = try { service.asBinder()?.isBinderAlive == true } catch (_: Exception) { false }
        val authorized = PowerModePolicy.mayUseBoundShizukuUserService(
            binderAlive, permissionGranted, uid, remoteBinderAlive, bound
        )
        if (!authorized) invalidateBoundService()
        return authorized
    }

    fun executeReadBack(commandId: String, callback: (String) -> Unit) {
        if (commandId !in READ_ONLY_COMMANDS) {
            callback("Blocked: only read-back command IDs are accepted here.")
            return
        }
        val service = remote
        if (service == null || !hasLiveServiceAuthorization(service)) {
            callback("Unavailable: Shizuku binder, owner authorization, UID 2000 and the bound service must all remain live.")
            return
        }
        val requestId = UUID.randomUUID().toString()
        activeRequests.add(requestId)
        worker.execute {
            val result = try {
                if (!hasLiveServiceAuthorization(service)) {
                    "rejected: Shizuku authorization or service connection was lost before dispatch"
                } else {
                    service.execute(requestId, commandId, emptyArray(), 4_000L, false)
                }
            } catch (e: Exception) {
                "failed: ${e.javaClass.simpleName}"
            } finally {
                activeRequests.remove(requestId)
            }
            mainHandler.post { callback(result) }
        }
    }

    fun installVerifiedApk(publicDownloadPath: String, expectedSha256: String, callback: (String) -> Unit) {
        val allowed = ShizukuCommandPolicy.command(
            commandId = "install_verified_apk",
            args = listOf(publicDownloadPath, expectedSha256),
            ownerConfirmed = true,
            packageName = "com.vyom.assistant"
        )
        if (allowed == null) {
            callback("rejected: APK path is outside the verified update allowlist")
            return
        }
        val service = remote
        if (service == null || !hasLiveServiceAuthorization(service)) {
            callback("unavailable: live Shizuku shell authorization and bound service are required")
            return
        }
        val requestId = UUID.randomUUID().toString()
        activeRequests.add(requestId)
        worker.execute {
            val result = try {
                if (!hasLiveServiceAuthorization(service)) {
                    "rejected: Shizuku authorization or service connection was lost before install"
                } else {
                    service.execute(requestId, "install_verified_apk", arrayOf(publicDownloadPath, expectedSha256), allowed.timeoutMs, true)
                }
            } catch (error: Exception) {
                "failed: ${error.javaClass.simpleName}"
            } finally {
                activeRequests.remove(requestId)
            }
            mainHandler.post { callback(result) }
        }
    }

    fun cancelAll() {
        val service = remote ?: return
        if (!hasLiveServiceAuthorization(service)) {
            emit("STOP could not reach Shizuku: authorization or service connection is no longer live.")
            return
        }
        activeRequests.toList().forEach { id ->
            try { service.cancel(id) } catch (_: Exception) { }
        }
        emit("STOP sent to active Shizuku shell requests.")
    }

    fun close() {
        cancelAll()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        if (bound) {
            try {
                val args = Shizuku.UserServiceArgs(
                    ComponentName(appContext.packageName, VyomShizukuUserService::class.java.name)
                ).daemon(false).processNameSuffix("vyom_shell")
                    .debuggable(BuildConfig.DEBUG).version(BuildConfig.VERSION_CODE)
                Shizuku.unbindUserService(args, this, true)
            } catch (_: Exception) { }
        }
        bound = false
        remote = null
        worker.shutdownNow()
    }

    private fun emit(message: String) {
        mainHandler.post { onStatus(message) }
    }

    private companion object {
        const val REQUEST_CODE = 7314
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        val READ_ONLY_COMMANDS = setOf(
            "verify_secure_settings",
            "verify_overlay",
            "verify_accessibility_enabled",
            "verify_accessibility_services"
        )
    }
}
