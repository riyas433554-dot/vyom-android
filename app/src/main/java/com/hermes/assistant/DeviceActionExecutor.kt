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
    fun execute(activity: Activity, action: ActionEngine.Intent): String = when (action.operation) {
        ActionEngine.Operation.BLOCK -> "Blocked by Vyom safety policy."
        ActionEngine.Operation.OPEN_SETTINGS -> openIntent(activity, Intent(action.settingsAction ?: Settings.ACTION_SETTINGS), action.name)
        ActionEngine.Operation.OPEN_APP -> openApp(activity, action.target)
        ActionEngine.Operation.VOLUME_DELTA -> {
            val a=activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val cur=a.getStreamVolume(AudioManager.STREAM_MUSIC); val max=a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            a.setStreamVolume(AudioManager.STREAM_MUSIC,(cur+action.value*maxOf(1,max/10)).coerceIn(0,max),0)
            if(action.value>0) "Media volume increased." else "Media volume decreased."
        }
        ActionEngine.Operation.VOLUME_SET -> {
            val a=activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager; val max=a.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            a.setStreamVolume(AudioManager.STREAM_MUSIC,(max*action.value/100f).toInt().coerceIn(0,max),0)
            "Media volume set to " + action.value + "%."
        }
        ActionEngine.Operation.BRIGHTNESS_SET -> setBrightness(activity,action.value)
        ActionEngine.Operation.TORCH -> setTorch(activity,action.value==1)
        ActionEngine.Operation.SET_ALARM -> {
            val p=action.target.split(":"); val h=p.getOrNull(0)?.toIntOrNull() ?: return "Alarm time invalid."; val m=p.getOrNull(1)?.toIntOrNull() ?: 0
            openIntent(activity,Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,h).putExtra(AlarmClock.EXTRA_MINUTES,m).putExtra(AlarmClock.EXTRA_MESSAGE,"Set by Vyom").putExtra(AlarmClock.EXTRA_SKIP_UI,false),"Alarm screen")
        }
        ActionEngine.Operation.SET_TIMER -> openIntent(activity,Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH,action.value.coerceIn(1,86400)).putExtra(AlarmClock.EXTRA_MESSAGE,"Set by Vyom").putExtra(AlarmClock.EXTRA_SKIP_UI,false),"Timer screen")
        ActionEngine.Operation.DIAL -> openIntent(activity,Intent(Intent.ACTION_DIAL,Uri.parse("tel:" + Uri.encode(action.target))),"Dialer")
        ActionEngine.Operation.SMS -> { val p=action.target.split("\u0000",limit=2); openIntent(activity,Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:" + Uri.encode(p.firstOrNull().orEmpty()))).putExtra("sms_body",p.getOrNull(1).orEmpty()),"SMS composer") }
        ActionEngine.Operation.SHARE -> { val t=action.target.replaceFirst(Regex("""^.*?\b(send|share)\s+""",RegexOption.IGNORE_CASE),"").trim(); openIntent(activity,Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,t),"Choose where to share"),"Share sheet") }
        ActionEngine.Operation.DELETE -> "Delete is not performed automatically. Use the relevant Android app or file manager."
        ActionEngine.Operation.INSTALL -> "Only a SHA-256 and signing-certificate verified Vyom update can be installed from Profile > Settings."
    }
    private fun openApp(a:Activity,n:String):String {
        val apps=a.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
        val wanted=n.lowercase().replace(Regex("[^a-z0-9]"),"")
        val hit=apps.firstOrNull { it.loadLabel(a.packageManager).toString().lowercase().replace(Regex("[^a-z0-9]"),"")==wanted }
            ?: apps.firstOrNull { val label=it.loadLabel(a.packageManager).toString().lowercase().replace(Regex("[^a-z0-9]"),""); label.contains(wanted)||wanted.contains(label) }
            ?: return "Couldn't find an installed app named '" + n + "'."
        val intent=a.packageManager.getLaunchIntentForPackage(hit.activityInfo.packageName) ?: return "That app doesn't provide a launch screen."
        return openIntent(a,intent,hit.loadLabel(a.packageManager).toString())
    }
    private fun setBrightness(a:Activity,p:Int):String {
        if(!Settings.System.canWrite(a)) {
            openIntent(a,Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:" + a.packageName)),"Modify system settings permission screen")
            return "Enable Modify system settings, then repeat the brightness command."
        }
        val v=(p.coerceIn(0,100)*255/100).coerceIn(0,255)
        return if(Settings.System.putInt(a.contentResolver,Settings.System.SCREEN_BRIGHTNESS,v)) "Screen brightness set to " + p + "%." else "Android refused to change screen brightness."
    }
    private fun setTorch(a:Activity,on:Boolean):String {
        if(a.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
            a.requestPermissions(arrayOf(Manifest.permission.CAMERA),704)
            return "Camera permission requested. Allow it, then repeat the torch command."
        }
        return try {
            val m=a.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id=m.cameraIdList.firstOrNull { m.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)==true } ?: return "This device has no available torch."
            m.setTorchMode(id,on)
            if(on) "Torch turned on." else "Torch turned off."
        } catch(e:Exception) { "Could not change torch: " + (e.message ?: "Android camera service error") }
    }
    private fun openIntent(a:Activity,i:Intent,label:String):String = try {
        if(i.resolveActivity(a.packageManager)==null) "No Android app can handle " + label + "."
        else { a.startActivity(i); label + " opened." }
    } catch(e:Exception) { "Could not open " + label + ": " + (e.message ?: "Android rejected the request") }
}