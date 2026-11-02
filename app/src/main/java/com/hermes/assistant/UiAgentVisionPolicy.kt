package com.hermes.assistant

/** Pure safety and geometry rules for screenshot-derived visual gestures. */
internal object UiAgentVisionPolicy {
    const val MODEL = "nvidia/nemotron-nano-12b-v2-vl"

    fun normalizedToPixels(value: Int, dimension: Int): Int? {
        if (value !in 0..1000 || dimension <= 0) return null
        return ((value.toLong() * (dimension - 1).toLong() + 500L) / 1000L).toInt()
    }

    fun isAspectCompatible(imageWidth: Int, imageHeight: Int, screenWidth: Int, screenHeight: Int): Boolean {
        if (imageWidth <= 0 || imageHeight <= 0 || screenWidth <= 0 || screenHeight <= 0) return false
        val imageCross = imageWidth.toLong() * screenHeight.toLong()
        val screenCross = screenWidth.toLong() * imageHeight.toLong()
        val difference = kotlin.math.abs(imageCross - screenCross)
        return difference * 100L <= maxOf(imageCross, screenCross) * 5L
    }

    fun hasSensitiveContext(labels: Collection<String>, hasSensitiveInputField: Boolean): Boolean {
        if (hasSensitiveInputField) return true
        val sensitive = Regex(
            """\b(password|passcode|otp|cvv|pin|api key|access token|secret|seed phrase|recovery phrase|private key|upi|wallet|bank|payment|pay|transfer|security|permission|biometric|card number|account number)\b""",
            RegexOption.IGNORE_CASE
        )
        val credentialLike = Regex("""\b\d{4,12}\b|\b(?:\d[ -]*?){13,19}\b|[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}""", RegexOption.IGNORE_CASE)
        return labels.any { sensitive.containsMatchIn(it) || credentialLike.containsMatchIn(it) }
    }

    fun isSafeGesture(
        kind: String, x1: Int, y1: Int, x2: Int, y2: Int,
        width: Int, height: Int, density: Float, sensitiveContext: Boolean
    ): Boolean {
        if (sensitiveContext || width <= 0 || height <= 0 || !density.isFinite() || density <= 0f) return false
        val inset = (24f * density).toInt().coerceAtLeast(12)
        if (width <= inset * 2 || height <= inset * 2) return false
        fun inside(x: Int, y: Int) = x >= inset && y >= inset && x < width - inset && y < height - inset
        if (!inside(x1, y1) || !inside(x2, y2)) return false
        val dx = (x2 - x1).toDouble()
        val dy = (y2 - y1).toDouble()
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        return when (kind.lowercase()) {
            "tap", "long_press" -> x1 == x2 && y1 == y2
            "swipe", "drag" -> distance >= 24.0 * density &&
                distance <= kotlin.math.sqrt(width.toDouble() * width + height.toDouble() * height) * 0.85
            else -> false
        }
    }
}
