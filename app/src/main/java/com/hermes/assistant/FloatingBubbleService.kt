package com.hermes.assistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.TextView

/** User-enabled overlay orb. It never opens the microphone or performs device actions. */
class FloatingBubbleService : Service() {
    private var windowManager: WindowManager? = null
    private var bubbleView: TextView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Vyom floating bubble", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while the user-enabled Vyom floating bubble is active"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!BuildConfig.ENABLE_FLOATING_BUBBLE ||
            !FloatingBubblePolicy.shouldStart(true, Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this))) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        if (bubbleView == null) attachBubble()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 801, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 802, Intent(this, FloatingBubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        return builder.setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Vyom floating bubble active")
            .setContentText("Tap the orb to open Vyom. Microphone is not active.")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Stop bubble", stop).build())
            .setOngoing(true)
            .build()
    }

    private fun attachBubble() {
        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = manager
        val view = TextView(this).apply {
            text = "◉"
            textSize = 23f
            gravity = Gravity.CENTER
            contentDescription = "Vyom floating assistant bubble. Tap to open Vyom; drag to move."
            setTextColor(android.graphics.Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xE6192D46.toInt())
                setStroke(dp(2), VyomTheme.ACCENT_CYAN)
            }
            elevation = dp(12).toFloat()
        }
        val params = WindowManager.LayoutParams(
            dp(58), dp(58),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = dp(12)
            y = 0
        }
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop) moved = true
                    if (moved) {
                        params.x = (startX - dx).coerceAtLeast(0)
                        params.y = startY + dy
                        try { manager.updateViewLayout(view, params) } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        startActivity(Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    }
                    true
                }
                else -> true
            }
        }
        try {
            manager.addView(view, params)
            bubbleView = view
            bubbleParams = params
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        try { bubbleView?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        bubbleView = null
        bubbleParams = null
        windowManager = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.hermes.assistant.bubble.STOP"
        private const val CHANNEL_ID = "vyom_floating_bubble"
        private const val NOTIFICATION_ID = 803
    }
}