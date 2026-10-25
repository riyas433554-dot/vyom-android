package com.hermes.assistant

import android.content.Context
import android.opengl.GLSurfaceView

class OrbSurfaceView(context: Context) : GLSurfaceView(context) {
    val orbRenderer = OrbRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(orbRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        isClickable = true
    }

    fun setOrbMode(mode: Int) {
        orbRenderer.mode = mode
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }
}
