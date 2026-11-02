package com.hermes.assistant

/** Pure geometry gate for screenshot-derived gestures. Coordinates are screen pixels. */
internal object UiAgentVisionPolicy {
    const val MODEL = "nvidia/nemotron-nano-12b-v2-vl"

    fun normalizedToPixels(value: Int, dimension: Int): Int? = null

    fun isSafeGesture(
        kind: String, x1: Int, y1: Int, x2: Int, y2: Int,
        width: Int, height: Int, density: Float, sensitiveContext: Boolean
    ): Boolean = false
}
