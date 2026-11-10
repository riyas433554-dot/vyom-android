package com.hermes.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import org.json.JSONArray
import org.json.JSONObject

/** Keeps the active voice session visible to Android while the user backgrounds Vyom. */
class VoiceForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Vyom voice session", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while Vyom voice listening is active"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_MUTE) {
            sendBroadcast(Intent(ACTION_SESSION_MUTE).setPackage(packageName))
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_CONTROL_STOP) {
            recordControlStop()
            sendBroadcast(Intent(ACTION_SESSION_CONTROL_STOP).setPackage(packageName))
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_STOP) {
            sendBroadcast(Intent(ACTION_SESSION_STOP).setPackage(packageName))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val stopIntent = PendingIntent.getService(
            this, 732, Intent(this, VoiceForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val muteIntent = PendingIntent.getService(
            this, 733, Intent(this, VoiceForegroundService::class.java).setAction(ACTION_MUTE),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val controlStopIntent = PendingIntent.getService(this, 734, Intent(this, VoiceForegroundService::class.java).setAction(ACTION_CONTROL_STOP), PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val notification = builder.setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Vyom voice is active").setContentText("Microphone session active")
            .addAction(Notification.Action.Builder(null, "Mute", muteIntent).build())
            .addAction(Notification.Action.Builder(null, "Stop", stopIntent).build())
            .addAction(Notification.Action.Builder(null, "STOP CONTROL", controlStopIntent).build())
            .setOngoing(true).build()
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    private fun recordControlStop() {
        val prefs = getSharedPreferences("vyom_control_audit", MODE_PRIVATE)
        val rows = try { JSONArray(prefs.getString("events", "[]")) } catch (_: Exception) { JSONArray() }
        val next = JSONArray()
        next.put(JSONObject().put("at", java.time.Instant.now().toString()).put("elapsedMs", 0)
            .put("source", "notification").put("action", "STOP CONTROL").put("result", "enabled"))
        for (i in 0 until minOf(rows.length(), 49)) next.put(rows.getJSONObject(i))
        prefs.edit().putBoolean("kill_switch", true).putString("events", next.toString()).apply()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.hermes.assistant.voice.STOP"
        const val ACTION_MUTE = "com.hermes.assistant.voice.MUTE"
        const val ACTION_CONTROL_STOP = "com.hermes.assistant.voice.CONTROL_STOP"
        const val ACTION_SESSION_STOP = "com.hermes.assistant.voice.SESSION_STOP"
        const val ACTION_SESSION_MUTE = "com.hermes.assistant.voice.SESSION_MUTE"
        const val ACTION_SESSION_CONTROL_STOP = "com.hermes.assistant.voice.SESSION_CONTROL_STOP"
        private const val CHANNEL_ID = "vyom_voice_session"
        private const val NOTIFICATION_ID = 731
    }
}
