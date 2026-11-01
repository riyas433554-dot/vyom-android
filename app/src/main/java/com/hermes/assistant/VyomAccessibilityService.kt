package com.hermes.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor

/**
 * Optional, explicitly user-enabled local accessibility bridge.
 * It can inspect visible UI nodes and perform a bounded action requested by Vyom.
 * Screenshots stay in app cache; this service never uploads screen data.
 */
class VyomAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        active = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event text is deliberately not forwarded to network or logs.
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (active === this) active = null
        super.onDestroy()
    }

    fun describeVisibleTree(maxNodes: Int = 180): JSONArray {
        val result = JSONArray()
        val root = rootInActiveWindow ?: return result
        var count = 0
        fun visit(node: AccessibilityNodeInfo?, parentIndex: Int) {
            if (node == null || count >= maxNodes) return
            val index = count++
            val password = node.isPassword
            val row = JSONObject()
                .put("index", index)
                .put("parent", parentIndex)
                .put("class", node.className?.toString().orEmpty().take(100))
                .put("clickable", node.isClickable)
                .put("longClickable", node.isLongClickable)
                .put("editable", node.isEditable)
                .put("scrollable", node.isScrollable)
                .put("password", password)
                .put("bounds", JSONObject().also { b ->
                    val rect = android.graphics.Rect()
                    node.getBoundsInScreen(rect)
                    b.put("left", rect.left).put("top", rect.top).put("right", rect.right).put("bottom", rect.bottom)
                })
            if (!password) {
                row.put("text", node.text?.toString()?.take(240).orEmpty())
                row.put("description", node.contentDescription?.toString()?.take(160).orEmpty())
            } else {
                row.put("text", "").put("description", "[REDACTED PASSWORD FIELD]")
            }
            result.put(row)
            for (i in 0 until node.childCount) visit(node.getChild(i), index)
        }
        visit(root, -1)
        return result
    }

    fun clickVisibleText(target: String): Boolean {
        if (target.isBlank() || target.length > 100) return false
        val root = rootInActiveWindow ?: return false
        val node = findNode(root, target) ?: return false
        if (node.isPassword || node.isEditable) return false
        var candidate: AccessibilityNodeInfo? = node
        while (candidate != null) {
            if (candidate.isClickable) return candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            candidate = candidate.parent
        }
        return false
    }

    fun longPress(x: Float, y: Float, durationMs: Long = 650L): Boolean =
        dispatchPath(x, y, x, y, durationMs.coerceIn(500L, 1500L))

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 350L): Boolean =
        dispatchPath(x1, y1, x2, y2, durationMs.coerceIn(100L, 1200L))

    fun scroll(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = when (direction.lowercase()) {
            "up", "forward", "down-page" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            "down", "backward", "up-page" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            else -> return false
        }
        val node = findScrollable(root) ?: root
        return node.performAction(action)
    }

    fun typeIntoFocusedField(text: String): Boolean {
        if (text.length > 2000 || containsCredentialPattern(text)) return false
        val root = rootInActiveWindow ?: return false
        val node = findFocusedEditable(root) ?: return false
        if (node.isPassword || !node.isEditable) return false
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun global(action: String): Boolean = when (action.lowercase()) {
        "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
        "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
        "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        else -> false
    }

    fun captureLocalScreenshot(onResult: (File?, String?) -> Unit) {
        if (Build.VERSION.SDK_INT < 30) { onResult(null, "Screenshot requires Android 11 or newer."); return }
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                try {
                    val bitmap = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                        ?: throw IllegalStateException("Screenshot buffer unavailable")
                    val file = File(cacheDir, "vyom-screen-${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { bitmap.copy(Bitmap.Config.ARGB_8888, false).compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                    screenshot.hardwareBuffer.close()
                    onResult(file, null)
                } catch (e: Exception) { onResult(null, e.message ?: "Could not save screenshot.") }
            }
            override fun onFailure(errorCode: Int) {
                onResult(null, if (errorCode == ERROR_TAKE_SCREENSHOT_SECURE_WINDOW) "Secure window screenshot blocked by Android." else "Screenshot failed (code $errorCode).")
            }
        })
    }

    private fun dispatchPath(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long): Boolean {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        return dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs)).build(), null, null)
    }

    private fun findNode(root: AccessibilityNodeInfo, target: String): AccessibilityNodeInfo? {
        val nodes = root.findAccessibilityNodeInfosByText(target)
        return nodes.firstOrNull { !it.isPassword && (it.text?.toString()?.equals(target, true) == true ||
            it.contentDescription?.toString()?.equals(target, true) == true) }
    }

    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) findScrollable(node.getChild(i))?.let { return it }
        return null
    }

    private fun findFocusedEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused && node.isEditable) return node
        for (i in 0 until node.childCount) findFocusedEditable(node.getChild(i))?.let { return it }
        return null
    }

    private fun containsCredentialPattern(text: String): Boolean =
        Regex("(?i)(password|passcode|otp|cvv|api[_ -]?key|access[_ -]?token)").containsMatchIn(text) ||
            Regex("\\b(?:\\d[ -]*?){13,19}\\b").containsMatchIn(text)

    companion object {
        @Volatile private var active: VyomAccessibilityService? = null
        fun current(): VyomAccessibilityService? = active
    }
}
