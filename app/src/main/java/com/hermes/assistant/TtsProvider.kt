package com.hermes.assistant

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

interface TtsProvider {
    fun speak(text: String, onComplete: () -> Unit, onError: (String) -> Unit)
    fun stop()
    fun status(): String
}

class AndroidTtsProvider(private val engine: TextToSpeech?) : TtsProvider {
    override fun speak(text: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        val tts = engine
        if (tts == null) { onError("Android TTS unavailable"); return }
        tts.language = Locale("en", "IN")
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onComplete() }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { onError("Android TTS failed") }
            override fun onError(utteranceId: String?, errorCode: Int) { onError("Android TTS failed ($errorCode)") }
        })
        val result = tts.speak(text.take(4000), TextToSpeech.QUEUE_FLUSH, null, "vyom-${System.nanoTime()}")
        if (result == TextToSpeech.ERROR) onError("Android TTS failed")
    }
    override fun stop() { engine?.stop() }
    override fun status() = "Android TTS fallback"
}

class ElevenLabsTtsProvider(
    private val context: Context,
    private val apiKey: String,
    private val voiceId: String,
    private val fallback: TtsProvider
) : TtsProvider {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var player: MediaPlayer? = null
    @Volatile private var stopped = false

    override fun status(): String = if (apiKey.isBlank() || voiceId.isBlank()) "ElevenLabs not configured — using Android TTS" else "ElevenLabs voice enabled"

    override fun speak(text: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        if (apiKey.isBlank() || voiceId.isBlank()) {
            fallback.speak(text, onComplete) { onError("Android TTS failed: $it") }
            return
        }
        stopped = false
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("https://api.elevenlabs.io/v1/text-to-speech/" + java.net.URLEncoder.encode(voiceId, "UTF-8")).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 12000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("xi-api-key", apiKey)
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "audio/mpeg")
                }
                val safeText = text.take(4000)
                val payload = org.json.JSONObject().put("text", safeText).put("model_id", "eleven_multilingual_v2")
                    .put("voice_settings", org.json.JSONObject().put("stability", 0.5).put("similarity_boost", 0.75))
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                if (connection.responseCode !in 200..299) throw IllegalStateException("ElevenLabs HTTP ${connection.responseCode}")
                val audioFile = File(context.cacheDir, "vyom-voice-${System.nanoTime()}.mp3")
                connection.inputStream.use { input -> audioFile.outputStream().use { output -> input.copyTo(output) } }
                if (stopped) { audioFile.delete(); return@execute }
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (stopped) { audioFile.delete(); return@post }
                    try {
                        player?.release()
                        player = MediaPlayer().apply {
                            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                            setDataSource(audioFile.absolutePath)
                            setOnCompletionListener { release(); player = null; audioFile.delete(); onComplete() }
                            setOnErrorListener { mp, _, _ -> mp.release(); player = null; audioFile.delete(); onError("ElevenLabs playback failed"); true }
                            prepare()
                            start()
                        }
                    } catch (_: Exception) { audioFile.delete(); fallback.speak(text, onComplete) { onError(it) } }
                }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    if (!stopped) fallback.speak(text, onComplete) { onError("ElevenLabs failed; Android TTS failed: $it") }
                }
            } finally { connection?.disconnect() }
        }
    }
    override fun stop() {
        stopped = true
        fallback.stop()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try { player?.stop() } catch (_: Exception) {}
            try { player?.release() } catch (_: Exception) {}
            player = null
        }
    }
}
