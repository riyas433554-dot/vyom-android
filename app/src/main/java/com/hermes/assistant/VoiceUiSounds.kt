package com.hermes.assistant

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

internal class VoiceUiSounds(context: Context) : AutoCloseable {
    private val pool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val muteId: Int
    private val unmuteId: Int
    @Volatile private var loaded = 0
    @Volatile private var pendingMute = false
    @Volatile private var pendingUnmute = false

    init {
        muteId = pool.load(context, R.raw.voice_mute, 1)
        unmuteId = pool.load(context, R.raw.voice_unmute, 1)
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) synchronized(this) {
                loaded++
                if (loaded >= 2) {
                    if (pendingMute) { pendingMute = false; play(muteId) }
                    if (pendingUnmute) { pendingUnmute = false; play(unmuteId) }
                }
            }
        }
    }
    @Synchronized fun playMuted() { if (loaded >= 2) play(muteId) else pendingMute = true }
    @Synchronized fun playUnmuted() { if (loaded >= 2) play(unmuteId) else pendingUnmute = true }
    private fun play(id: Int) { pool.play(id, 0.65f, 0.65f, 1, 0, 1f) }
    override fun close() { pool.release() }
}
