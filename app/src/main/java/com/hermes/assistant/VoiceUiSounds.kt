package com.hermes.assistant

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin

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
        val dir = File(context.cacheDir, "voice-ui-tones").apply { mkdirs() }
        muteId = pool.load(writeTone(File(dir, "mute.wav"), 520.0), 1)
        unmuteId = pool.load(writeTone(File(dir, "unmute.wav"), 780.0), 1)
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
    private fun writeTone(file: File, frequency: Double): String {
        val rate = 22050
        val count = (rate * 0.12).toInt()
        FileOutputStream(file).use { out ->
            fun int(v: Int) { repeat(4) { out.write((v shr (it * 8)) and 255) } }
            fun short(v: Int) { out.write(v and 255); out.write((v shr 8) and 255) }
            out.write("RIFF".toByteArray()); int(36 + count * 2)
            out.write("WAVEfmt ".toByteArray()); int(16); short(1); short(1)
            int(rate); int(rate * 2); short(2); short(16)
            out.write("data".toByteArray()); int(count * 2)
            for (i in 0 until count) {
                val envelope = minOf(1.0, i / 220.0, (count - i) / 220.0).coerceAtLeast(0.0)
                short((sin(2.0 * PI * frequency * i / rate) * envelope * 9000).toInt())
            }
        }
        return file.absolutePath
    }
    override fun close() { pool.release() }
}
