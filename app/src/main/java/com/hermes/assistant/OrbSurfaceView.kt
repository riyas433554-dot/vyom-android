package com.hermes.assistant

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView

class OrbSurfaceView(context: Context) : GLSurfaceView(context) {
    val orbRenderer = OrbRenderer()

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderMediaOverlay(true)
        setRenderer(orbRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        isClickable = true
    }

    fun setOrbMode(mode: Int) {
        orbRenderer.mode = mode
    }

    fun setReduceMotion(enabled: Boolean) {
        orbRenderer.reduceMotion = enabled
    }

    fun setLiteMode(enabled: Boolean) {
        orbRenderer.liteMode = enabled
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }
}
