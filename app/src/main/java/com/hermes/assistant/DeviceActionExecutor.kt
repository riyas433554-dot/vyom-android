package com.hermes.assistant

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings

internal object DeviceActionExecutor {
    fun execute(activity: Activity, action: ActionEngine.Intent): String { return when (action.operation) {
        ActionEngine.Operation.BLOCK -> "Blocked: Vyom safety policy."
        ActionEngine.Operation.INSTALL_PLAY_STORE -> "Pending verification: Play Store install flow must be opened after explicit confirmation."
        ActionEngine.Operation.OPEN_SETTINGS -> openIntent(activity, Intent(action.settingsAction ?: Settings.ACTION_SETTINGS), action.name)
        ActionEngine.Operation.OPEN_APP -> openApp(activity, action.target)
        ActionEngine.Operation.VOLUME_DELTA -> {
            val a=activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val cur=a.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max=a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val requested=(cur+action.value*maxOf(1,max/10)).coerceIn(0,max)
            return try {
                a.setStreamVolume(AudioManager.STREAM_MUSIC,requested,0)
                val actual=a.getStreamVolume(AudioManager.STREAM_MUSIC)
                if(actual==requested) {
                    val detail=if(actual==cur && action.value>0) "already at maximum" else if(actual==cur && action.value<0) "already at minimum" else "changed to $actual/$max"
                    "Verified: Media volume $detail."
                } else "Not verified: requested media volume $requested/$max, Android reports $actual/$max."
            } catch(e:Exception) { "Failed: Could not change media volume: " + (e.message ?: "Android rejected the request") }
        }
        ActionEngine.Operation.VOLUME_SET -> {
            val a=activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max=a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val requested=(max*action.value/100f).toInt().coerceIn(0,max)
            return try {
                a.setStreamVolume(AudioManager.STREAM_MUSIC,requested,0)
                val actual=a.getStreamVolume(AudioManager.STREAM_MUSIC)
                if(actual==requested) "Verified: Media volume is $actual/$max (requested ${action.value}%)."
                else "Not verified: requested media volume $requested/$max, Android reports $actual/$max."
            } catch(e:Exception) { "Failed: Could not set media volume: " + (e.message ?: "Android rejected the request") }
        }
        ActionEngine.Operation.VOLUME_MUTE -> {
            val a=activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val prefs=activity.getSharedPreferences("vyom_device_audio",Context.MODE_PRIVATE)
            val current=a.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max=a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            if(current>0) prefs.edit().putInt("last_nonzero_media_volume",current).apply()
            return try {
                a.setStreamVolume(AudioManager.STREAM_MUSIC,0,0)
                val actual=a.getStreamVolume(AudioManager.STREAM_MUSIC)
                if(actual==0) "Verified: Media volume is muted (0/$max)."
                else "Not verified: mute requested, Android reports $actual/$max."
            } catch(e:Exception) { "Failed: Could not mute media volume: " + (e.message ?: "Android rejected the request") }
        }
        ActionEngine.Operation.VOLUME_UNMUTE -> {
            val a=activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max=a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val restore=activity.getSharedPreferences("vyom_device_audio",Context.MODE_PRIVATE).getInt("last_nonzero_media_volume",maxOf(1,max/3)).coerceIn(1,max)
            return try {
                a.setStreamVolume(AudioManager.STREAM_MUSIC,restore,0)
                val actual=a.getStreamVolume(AudioManager.STREAM_MUSIC)
                if(actual==restore) "Verified: Media volume restored to $actual/$max."
                else "Not verified: unmute requested at $restore/$max, Android reports $actual/$max."
            } catch(e:Exception) { "Failed: Could not unmute media volume: " + (e.message ?: "Android rejected the request") }
        }
        ActionEngine.Operation.BRIGHTNESS_SET -> setBrightness(activity,action.value)
        ActionEngine.Operation.TORCH -> setTorch(activity,action.value==1)
        ActionEngine.Operation.SET_ALARM -> {
            val p=action.target.split(":"); val h=p.getOrNull(0)?.toIntOrNull() ?: return "Failed: Alarm time invalid."; val m=p.getOrNull(1)?.toIntOrNull() ?: 0
            openIntent(activity,Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,h).putExtra(AlarmClock.EXTRA_MINUTES,m).putExtra(AlarmClock.EXTRA_MESSAGE,"Set by Vyom").putExtra(AlarmClock.EXTRA_SKIP_UI,false),"Alarm screen","Pending verification: Alarm setup screen requested; no alarm is set until you confirm it on Android.")
        }
        ActionEngine.Operation.SET_TIMER -> openIntent(activity,Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH,action.value.coerceIn(1,86400)).putExtra(AlarmClock.EXTRA_MESSAGE,"Set by Vyom").putExtra(AlarmClock.EXTRA_SKIP_UI,false),"Timer screen","Pending verification: Timer screen requested; no timer is set until you confirm it on Android.")
        ActionEngine.Operation.DIAL -> openIntent(activity,Intent(Intent.ACTION_DIAL,Uri.parse("tel:" + Uri.encode(action.target))),"Dialer","Pending verification: Dialer requested; no call has been placed.")
        ActionEngine.Operation.SMS -> { val p=action.target.split("\u0000",limit=2); openIntent(activity,Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:" + Uri.encode(p.firstOrNull().orEmpty()))).putExtra("sms_body",p.getOrNull(1).orEmpty()),"SMS composer","Pending verification: SMS composer requested; nothing has been sent.")
        }
        ActionEngine.Operation.SHARE -> { val t=action.target.replaceFirst(Regex("""^.*?\b(send|share)\s+""",RegexOption.IGNORE_CASE),"").trim(); openIntent(activity,Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,t),"Choose where to share"),"Share sheet","Pending verification: Share sheet requested; nothing has been shared.") }
        ActionEngine.Operation.DELETE -> "Delete is not performed automatically. Use the relevant Android app or file manager."
        ActionEngine.Operation.UNINSTALL -> {
            val resolved = resolveInstalledPackage(activity, action.target)
                ?: return "Failed: Could not uniquely identify installed app '${action.target}'. Use its exact app name or package ID."
            if (resolved == activity.packageName) return "Blocked: Vyom cannot uninstall itself or its protected update core."
            if (AppAccessPolicy.isBlocked(activity, resolved)) return "Blocked: Banking, wallet, payment and authenticator apps are protected."
            openIntent(
                activity,
                Intent(Intent.ACTION_DELETE, Uri.parse("package:$resolved")),
                "Uninstall ${action.target}",
                "Pending verification: Android uninstall confirmation opened for ${action.target}; Android still requires its own confirmation."
            )
        }
        ActionEngine.Operation.INSTALL -> "Only a SHA-256 and signing-certificate verified Vyom update can be installed from Profile > Settings."
    }
    }
    private fun resolveInstalledPackage(a: Activity, query: String): String? {
        val normalized = query.trim()
        if (normalized.matches(Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$"))) {
            return try {
                a.packageManager.getPackageInfo(normalized, 0)
                normalized
            } catch (_: Exception) { null }
        }
        val matches = try {
            a.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { info -> a.packageManager.getApplicationLabel(info).toString().equals(normalized, ignoreCase = true) }
                .map { it.packageName }.distinct()
        } catch (_: Exception) { emptyList() }
        return matches.singleOrNull()
    }

    private fun openApp(a:Activity,n:String):String {
        val pm=a.packageManager
        val pkg=DeviceCommandConfig.packageForApp(n)
        if(pkg!=null) {
            val launch=try { pm.getLaunchIntentForPackage(pkg) } catch(_:Exception) { null }
            if(launch!=null) return openIntent(a,launch,DeviceCommandConfig.displayNameForPackage(pkg) ?: n)
            val installed=try { pm.getPackageInfo(pkg,0); true } catch(_:PackageManager.NameNotFoundException) { false } catch(_:Exception) { false }
            if(!installed) return "Failed: App not installed: " + (DeviceCommandConfig.displayNameForPackage(pkg) ?: n) + " (" + pkg + "). Android launcher visibility queries are enabled; install the app or check its package."
            return "Failed: App is installed but has no visible launcher activity: " + (DeviceCommandConfig.displayNameForPackage(pkg) ?: n) + "."
        }
        val launcher=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps=try { pm.queryIntentActivities(launcher,0) } catch(_:Exception) { emptyList() }
        val hit=apps.map { it to it.loadLabel(pm).toString() }
            .map { (info,label) -> Triple(info,label,DeviceCommandConfig.similarityScore(n,label)) }
            .filter { it.third >= DeviceCommandConfig.minimumAppMatchScore(n) }
            .maxByOrNull { it.third }
            ?: return "Failed: No matching launcher app is visible. '" + n + "' may not be installed, or Android package visibility may hide it; launcher visibility queries are enabled."
        val intent=pm.getLaunchIntentForPackage(hit.first.activityInfo.packageName) ?: return "Failed: App is visible but has no launch screen: " + hit.second + "."
        return openIntent(a,intent,hit.second)
    }
    private fun setBrightness(a:Activity,p:Int):String {
        if(!Settings.System.canWrite(a)) {
            return openIntent(a,Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:" + a.packageName)),"Modify system settings permission screen","Pending permission: Enable Modify system settings, then repeat the brightness command.")
        }
        val v=(p.coerceIn(0,100)*255/100).coerceIn(0,255)
        return try {
            if(!Settings.System.putInt(a.contentResolver,Settings.System.SCREEN_BRIGHTNESS,v)) return "Not verified: Android refused to change screen brightness."
            val actual=Settings.System.getInt(a.contentResolver,Settings.System.SCREEN_BRIGHTNESS,-1)
            if(actual==v) "Verified: Screen brightness is $p% ($actual/255)."
            else "Not verified: requested brightness $v/255, Android reports $actual/255."
        } catch(e:Exception) { "Failed: Could not set screen brightness: " + (e.message ?: "Android rejected the request") }
    }
    private fun setTorch(a:Activity,on:Boolean):String {
        if(a.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
            a.requestPermissions(arrayOf(Manifest.permission.CAMERA),704)
            return "Pending permission: Camera permission requested. Allow it, then repeat the torch command."
        }
        return try {
            val m=a.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id=m.cameraIdList.firstOrNull { m.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)==true } ?: return "Failed: This device has no available torch."
            m.setTorchMode(id,on)
            "Pending verification: Android accepted the torch ${if(on) "ON" else "OFF"} request; the torch state callback has not yet been independently observed."
        } catch(e:Exception) { "Failed: Could not change torch: " + (e.message ?: "Android camera service error") }
    }
    private fun openIntent(
        a:Activity,
        i:Intent,
        label:String,
        pendingMessage:String = "Pending verification: Android launch requested for $label; foreground package has not yet been confirmed."
    ):String {
        return try {
            val resolved=i.resolveActivity(a.packageManager)
                ?: return "Failed: No Android app can handle $label."
            a.startActivity(i)
            "$pendingMessage (handler: ${resolved.packageName})."
        } catch(e:Exception) { "Failed: Could not open $label: " + (e.message ?: "Android rejected the request") }
    }

    internal fun auditState(message:String):String = when {
        message.startsWith("Verified:", ignoreCase = true) -> "verified"
        message.startsWith("Pending verification:", ignoreCase = true) -> "verification-pending"
        message.startsWith("Pending permission:", ignoreCase = true) -> "permission-required"
        message.startsWith("Not verified:", ignoreCase = true) -> "not-verified"
        message.startsWith("Blocked:", ignoreCase = true) ||
            message.startsWith("Delete is not performed", ignoreCase = true) ||
            message.startsWith("Only a SHA-256", ignoreCase = true) -> "blocked"
        message.startsWith("Failed:", ignoreCase = true) ||
            message.startsWith("Could not", ignoreCase = true) ||
            message.startsWith("No Android", ignoreCase = true) ||
            message.startsWith("Android refused", ignoreCase = true) -> "failed"
        else -> "unverified"
    }
}