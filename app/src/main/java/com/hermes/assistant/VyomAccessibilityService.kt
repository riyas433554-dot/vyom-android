package com.hermes.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.Executor

/**
 * Optional, explicitly user-enabled local accessibility bridge.
 * It can inspect visible UI nodes and perform a bounded action requested by Vyom.
 * Screenshots are processed locally and never uploaded. Filtered OCR labels may be sent to the configured planner by the app.
 */
class VyomAccessibilityService : AccessibilityService() {
    data class OcrItem(val index: Int, val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val fingerprint: String get() = listOf(text, left, top, right, bottom).joinToString("|")
    }

    @Volatile private var screenRevision = 0L
    @Volatile private var lastOcrRevision = -1L
    @Volatile private var lastOcrPackage = ""
    @Volatile private var lastOcrTargets: List<OcrItem> = emptyList()
    private var agentOverlayRoot: LinearLayout? = null
    private var agentOverlayStatus: TextView? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        active = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event text is deliberately not forwarded to network or logs.
        if (event != null && event.packageName?.toString() == lastOcrPackage &&
            lastOcrPackage.isNotBlank() && lastOcrPackage != packageName &&
            event.eventType in setOf(
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_WINDOWS_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            )) screenRevision++
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        hideAgentOverlay()
        if (active === this) active = null
        super.onDestroy()
    }

    fun showAgentProgress(message: String, onStop: () -> Unit): Boolean =
        showAgentOverlay(message, listOf("STOP" to onStop))

    fun updateAgentProgress(message: String) {
        agentOverlayStatus?.text = message
    }

    fun showAgentConfirmation(message: String, onConfirm: () -> Unit, onCancel: () -> Unit): Boolean =
        showAgentOverlay(message, listOf("CANCEL" to onCancel, "CONFIRM" to onConfirm))

    fun showAgentFinished(message: String): Boolean =
        showAgentOverlay(message, listOf("CLOSE" to { hideAgentOverlay() }))

    fun hideAgentOverlay() {
        val root = agentOverlayRoot ?: return
        try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(root) } catch (_: Exception) { }
        agentOverlayRoot = null
        agentOverlayStatus = null
    }

    private fun showAgentOverlay(message: String, actions: List<Pair<String, () -> Unit>>): Boolean {
        hideAgentOverlay()
        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14 * density).toInt(), (10 * density).toInt(), (14 * density).toInt(), (10 * density).toInt())
            background = GradientDrawable().apply {
                setColor(0xF0101A2E.toInt())
                cornerRadius = 18 * density
                setStroke((1 * density).toInt(), 0xFF22D3EE.toInt())
            }
            elevation = 8 * density
        }
        val status = TextView(this).apply {
            text = message
            textSize = 13f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, (8 * density).toInt())
        }
        root.addView(status, LinearLayout.LayoutParams(-1, -2))
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        actions.forEach { (label, action) ->
            val button = Button(this).apply {
                text = label
                isAllCaps = false
                setOnClickListener { action() }
            }
            buttons.addView(button, LinearLayout.LayoutParams(-2, (40 * density).toInt()).apply {
                leftMargin = (6 * density).toInt()
            })
        }
        root.addView(buttons, LinearLayout.LayoutParams(-1, -2))
        val availableWidth = (resources.displayMetrics.widthPixels - (32 * density).toInt()).coerceAtLeast((220 * density).toInt())
        val params = WindowManager.LayoutParams(
            availableWidth.coerceAtMost((360 * density).toInt()),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (48 * density).toInt()
        }
        return try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).addView(root, params)
            agentOverlayRoot = root
            agentOverlayStatus = status
            true
        } catch (_: Exception) {
            false
        }
    }

    fun describeVisibleTree(maxNodes: Int = 180): JSONArray {
        val result = JSONArray()
        val root = rootInActiveWindow ?: return result
        if (!isCurrentAppAllowed(root)) return result
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
                .put("fingerprint", nodeFingerprint(node, root.packageName?.toString().orEmpty()))
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
        if (!isCurrentAppAllowed(root)) return false
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
        isCurrentAppAllowed() && dispatchPath(x, y, x, y, durationMs.coerceIn(500L, 1500L))

    fun clickNode(index: Int, expectedFingerprint: String): Boolean = performNodeAction(index, expectedFingerprint, AccessibilityNodeInfo.ACTION_CLICK, requireClickable = true)

    fun longPressNode(index: Int, expectedFingerprint: String): Boolean = performNodeAction(index, expectedFingerprint, AccessibilityNodeInfo.ACTION_LONG_CLICK, requireLongClickable = true)

    fun swipeNode(index: Int, expectedFingerprint: String, direction: String): Boolean {
        if (direction.lowercase() !in setOf("left", "right", "up", "down")) return false
        val root = rootInActiveWindow ?: return false
        if (!isCurrentAppAllowed(root)) return false
        val node = findNodeByIndex(root, index) ?: return false
        if (node.isPassword || node.isEditable || nodeFingerprint(node, root.packageName?.toString().orEmpty()) != expectedFingerprint) return false
        val rect = safeGestureBounds(node) ?: return false
        val x1: Float
        val y1: Float
        val x2: Float
        val y2: Float
        when (direction.lowercase()) {
            "left" -> { y1 = rect.centerY().toFloat(); y2 = y1; x1 = rect.right - rect.width() * 0.08f; x2 = rect.left + rect.width() * 0.08f }
            "right" -> { y1 = rect.centerY().toFloat(); y2 = y1; x1 = rect.left + rect.width() * 0.08f; x2 = rect.right - rect.width() * 0.08f }
            "up" -> { x1 = rect.centerX().toFloat(); x2 = x1; y1 = rect.bottom - rect.height() * 0.08f; y2 = rect.top + rect.height() * 0.08f }
            else -> { x1 = rect.centerX().toFloat(); x2 = x1; y1 = rect.top + rect.height() * 0.08f; y2 = rect.bottom - rect.height() * 0.08f }
        }
        return dispatchPath(x1, y1, x2, y2, 350L)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 350L): Boolean =
        isCurrentAppAllowed() && dispatchPath(x1, y1, x2, y2, durationMs.coerceIn(100L, 1200L))

    fun scroll(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        if (!isCurrentAppAllowed(root)) return false
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
        if (!isCurrentAppAllowed(root)) return false
        val node = findFocusedEditable(root) ?: return false
        if (node.isPassword || !node.isEditable) return false
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun global(action: String): Boolean {
        if (!isCurrentAppAllowed()) return false
        return when (action.lowercase()) {
        "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
        "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
        "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        else -> false
        }
    }

    fun captureVisibleOcr(onResult: (List<OcrItem>, String?) -> Unit) {
        if (!isCurrentAppAllowed()) { onResult(emptyList(), "App access is OFF or STOP CONTROL is ON."); return }
        if (Build.VERSION.SDK_INT < 30) { onResult(emptyList(), "OCR screenshot requires Android 11 or newer."); return }
        val rootAtCapture = rootInActiveWindow
        if (!isCurrentAppAllowed(rootAtCapture)) { onResult(emptyList(), "Current app is not enabled."); return }
        val packageAtCapture = rootAtCapture?.packageName?.toString().orEmpty()
        lastOcrPackage = packageAtCapture
        val revisionAtCapture = screenRevision
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                val bitmap = try {
                    val hardware = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                        ?: throw IllegalStateException("Screenshot buffer unavailable")
                    val copy = hardware.copy(Bitmap.Config.ARGB_8888, false)
                        ?: throw IllegalStateException("Could not copy screenshot for local OCR")
                    screenshot.hardwareBuffer.close()
                    hardware.recycle()
                    copy
                } catch (e: Exception) {
                    try { screenshot.hardwareBuffer.close() } catch (_: Exception) { }
                    onResult(emptyList(), e.message ?: "OCR screenshot unavailable.")
                    return
                }
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener(mainExecutor) { visionText ->
                        val targets = mutableListOf<OcrItem>()
                        for (block in visionText.textBlocks) {
                            for (line in block.lines) {
                                val value = line.text.trim()
                                val rect = line.boundingBox ?: continue
                                if (value.length !in 2..64 || isSensitiveOcrText(value)) continue
                                if (rect.left < 0 || rect.top < 0 || rect.right > bitmap.width || rect.bottom > bitmap.height) continue
                                targets.add(OcrItem(targets.size, value, rect.left, rect.top, rect.right, rect.bottom))
                                if (targets.size >= 80) break
                            }
                            if (targets.size >= 80) break
                        }
                        lastOcrTargets = targets
                        lastOcrPackage = packageAtCapture
                        lastOcrRevision = revisionAtCapture
                        onResult(targets, null)
                    }
                    .addOnFailureListener(mainExecutor) { error ->
                        lastOcrTargets = emptyList()
                        lastOcrPackage = packageAtCapture
                        lastOcrRevision = revisionAtCapture
                        onResult(emptyList(), error.message ?: "Local OCR failed.")
                    }
                    .addOnCompleteListener(mainExecutor) {
                        recognizer.close()
                        bitmap.recycle()
                    }
            }

            override fun onFailure(errorCode: Int) {
                lastOcrTargets = emptyList()
                lastOcrPackage = packageAtCapture
                lastOcrRevision = revisionAtCapture
                onResult(emptyList(), "OCR screenshot failed (code $errorCode).")
            }
        })
    }

    fun tapOcrTarget(expectedFingerprint: String): Boolean {
        val root = rootInActiveWindow ?: return false
        if (expectedFingerprint.isBlank() || root.packageName?.toString() != lastOcrPackage ||
            !isCurrentAppAllowed(root) || screenRevision != lastOcrRevision) return false
        val target = lastOcrTargets.firstOrNull { it.fingerprint == expectedFingerprint } ?: return false
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val targetWidth = target.right - target.left
        val targetHeight = target.bottom - target.top
        val area = targetWidth.toLong() * targetHeight.toLong()
        val screenArea = width.toLong() * height.toLong()
        if (target.left < 0 || target.top < 0 || target.right > width || target.bottom > height ||
            targetWidth < (8 * resources.displayMetrics.density).toInt() ||
            targetHeight < (8 * resources.displayMetrics.density).toInt() ||
            area <= 0 || area > screenArea * 10L / 100L) return false
        val x = (target.left + target.right) / 2f
        val y = (target.top + target.bottom) / 2f
        return dispatchPath(x, y, x, y, 90L)
    }

    private fun isSensitiveOcrText(value: String): Boolean =
        Regex("(?i)(password|passcode|otp|cvv|api[_ -]?key|access[_ -]?token|account number|card number)").containsMatchIn(value) ||
            Regex("""\b\d{4,12}\b""").containsMatchIn(value) ||
            Regex("""\b(?:\d[ -]*?){13,19}\b""").containsMatchIn(value) ||
            Regex("""\+?\d[\d\s().-]{7,}\d""").containsMatchIn(value) ||
            Regex("""[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}""", RegexOption.IGNORE_CASE).containsMatchIn(value) ||
            Regex("""\b[A-Za-z0-9_-]{24,}\b""").containsMatchIn(value)

    fun captureLocalScreenshot(onResult: (File?, String?) -> Unit) {
        if (!isCurrentAppAllowed()) { onResult(null, "App access is OFF or STOP CONTROL is ON."); return }
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

    private fun performNodeAction(index: Int, expectedFingerprint: String, action: Int, requireClickable: Boolean = false, requireLongClickable: Boolean = false): Boolean {
        if (index !in 0 until 180 || expectedFingerprint.isBlank()) return false
        val root = rootInActiveWindow ?: return false
        if (!isCurrentAppAllowed(root)) return false
        val node = findNodeByIndex(root, index) ?: return false
        if (node.isPassword || node.isEditable || nodeFingerprint(node) != expectedFingerprint || safeGestureBounds(node) == null) return false
        if (requireClickable && !node.isClickable) {
            val rect = safeGestureBounds(node) ?: return false
            return dispatchPath(rect.centerX().toFloat(), rect.centerY().toFloat(), rect.centerX().toFloat(), rect.centerY().toFloat(), 90L)
        }
        if (requireLongClickable && !node.isLongClickable) {
            val rect = safeGestureBounds(node) ?: return false
            return dispatchPath(rect.centerX().toFloat(), rect.centerY().toFloat(), rect.centerX().toFloat(), rect.centerY().toFloat(), 650L)
        }
        return node.performAction(action)
    }

    private fun findNodeByIndex(root: AccessibilityNodeInfo, targetIndex: Int): AccessibilityNodeInfo? {
        var count = 0
        fun visit(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null || count >= 180) return null
            val index = count++
            if (index == targetIndex) return node
            for (i in 0 until node.childCount) visit(node.getChild(i))?.let { return it }
            return null
        }
        return visit(root)
    }

    private fun safeGestureBounds(node: AccessibilityNodeInfo): android.graphics.Rect? {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val area = rect.width().toLong() * rect.height().toLong()
        val screenArea = width.toLong() * height.toLong()
        if (rect.left < 0 || rect.top < 0 || rect.right > width || rect.bottom > height ||
            rect.width() < (24 * resources.displayMetrics.density).toInt() ||
            rect.height() < (24 * resources.displayMetrics.density).toInt() ||
            area <= 0 || area > screenArea * 35L / 100L) return null
        return rect
    }

    private fun nodeFingerprint(node: AccessibilityNodeInfo, packageName: String): String {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        val visibleLabel = if (!node.isPassword && !node.isEditable) {
            node.text?.toString().orEmpty() + "|" + node.contentDescription?.toString().orEmpty()
        } else ""
        val digest = MessageDigest.getInstance("SHA-256").digest(visibleLabel.toByteArray(Charsets.UTF_8))
        val labelHash = digest.take(8).joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return listOf(packageName, node.className?.toString().orEmpty(), rect.left, rect.top, rect.right, rect.bottom,
            node.isClickable, node.isLongClickable, node.isPassword, node.isEditable, labelHash).joinToString("|")
    }

    private fun isCurrentAppAllowed(root: AccessibilityNodeInfo? = rootInActiveWindow): Boolean {
        if (getSharedPreferences("vyom_control_audit", MODE_PRIVATE).getBoolean("kill_switch", false)) return false
        val packageName = root?.packageName?.toString().orEmpty()
        return AppAccessPolicy.isAllowed(this, packageName)
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
