package com.hermes.assistant

import android.content.Context
import android.content.SharedPreferences

/** Persistent Factory-wide STOP latch shared by foreground and WorkManager workers. */
internal class VyomFactoryWorkerStopStore internal constructor(
    private val prefs: SharedPreferences
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    )

    fun stopAll() {
        check(prefs.edit().putBoolean(KEY_STOPPED, true).commit()) {
            "Factory global STOP could not be persisted"
        }
    }

    fun resumeAll() {
        check(prefs.edit().putBoolean(KEY_STOPPED, false).commit()) {
            "Factory workers could not be resumed"
        }
    }

    fun isStopped(): Boolean = prefs.getBoolean(KEY_STOPPED, false)

    companion object {
        private const val PREFS = "vyom_factory_worker_stop"
        private const val KEY_STOPPED = "stop_all_workers_v1"
    }
}
