package com.hermes.assistant

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.sqrt

/**
 * First-proof capture path: Android AudioRecord -> local RMS VAD -> ElevenLabs realtime STT.
 * API key is supplied by the caller from encrypted device preferences; never persisted here.
 */
internal class LiveSttCapture(
    private val context: Context,
    private val apiKey: String,
    private val languageCode: String = "",
    private val onPartial: (String) -> Unit,
    private val onCommitted: (String) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private val sampleRate = 16_000
    private val silenceMs = 700L
    private val speechThresholdRms = 780.0
    private var noiseFloorRms = 180.0
    @Volatile private var running = false
    @Volatile private var socketReady = false
    @Volatile private var offlineReady = false
    @Volatile private var offlineStt: VoskOfflineStt? = null
    @Volatile private var socket: WebSocket? = null
    @Volatile private var recorder: AudioRecord? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()
    private var lastSpeechAt = 0L
    private var segmentActive = false

    fun start() {
        if (running) return
        running = true
        if (apiKey.isBlank()) {
            onStatus("NO LIVE STT KEY — PREPARING OFFLINE ENGLISH STT")
            thread(name = "vyom-vosk-prepare") {
                val fallback = VoskOfflineStt(context)
                if (fallback.prepare(onStatus)) {
                    offlineStt?.close()
                    offlineStt = fallback
                    offlineReady = true
                    onStatus("OFFLINE ENGLISH STT READY — AUDIO STAYS ON DEVICE")
                } else {
                    fallback.close()
                    onStatus("OFFLINE STT UNAVAILABLE")
                }
            }
            thread(name = "vyom-audio-vad") { captureLoop() }
            return
        }
        val request = Request.Builder()
            .url("wss://api.elevenlabs.io/v1/speech-to-text/realtime?model_id=scribe_v2_realtime&audio_format=pcm_16000&commit_strategy=manual&keepalive_interval_ms=1000" +
                if (languageCode.isBlank()) "" else "&language_code=" + java.net.URLEncoder.encode(languageCode, "UTF-8"))
            .header("xi-api-key", apiKey)
            .build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                socketReady = true
                onStatus(if (languageCode.isBlank()) "LIVE STT CONNECTED — AUTO LANGUAGE" else "LIVE STT CONNECTED — LANGUAGE: $languageCode")
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val event = JSONObject(text)
                    when (event.optString("message_type")) {
                        "session_started" -> onStatus("LIVE STT READY — SPEAK NOW")
                        "partial_transcript" -> onPartial(cleanTranscript(event.optString("text")))
                        "audio_event" -> onStatus("STT AUDIO EVENT IGNORED")
                        "committed_transcript" -> {
                            val finalText = cleanTranscript(event.optString("text"))
                            if (finalText.isNotBlank()) onCommitted(finalText)
                        }
                        "error", "rate_limited" -> onStatus("LIVE STT ERROR: " + (event.optString("error").ifBlank { event.optString("message_type") }))
                    }
                } catch (_: Exception) { onStatus("LIVE STT RESPONSE ERROR") }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                socketReady = false
                onStatus("LIVE STT FAILED — PREPARING OFFLINE ENGLISH STT")
                thread(name = "vyom-vosk-prepare") {
                    val fallback = VoskOfflineStt(context)
                    if (fallback.prepare(onStatus)) {
                        offlineStt?.close()
                        offlineStt = fallback
                        offlineReady = true
                        onStatus("OFFLINE ENGLISH STT READY — AUDIO STAYS ON DEVICE")
                    } else {
                        fallback.close()
                        onStatus("LIVE AND OFFLINE STT UNAVAILABLE")
                    }
                }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socketReady = false
            }
        })
        thread(name = "vyom-audio-vad") { captureLoop() }
    }

    private fun captureLoop() {
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuffer <= 0) {
            onStatus("MIC INIT FAILED")
            stop()
            return
        }
        val bufferSize = maxOf(minBuffer, 3200)
        val audio = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        } catch (e: Exception) {
            onStatus("MIC INIT FAILED: ${e.message ?: "unavailable"}")
            stop()
            return
        }
        recorder = audio
        try {
            audio.startRecording()
            onStatus("MIC LISTENING — VAD ACTIVE")
            val buffer = ShortArray(1600) // 100 ms PCM frames
            while (running && audio.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val count = audio.read(buffer, 0, buffer.size)
                if (count <= 0 || (!socketReady && !offlineReady)) continue
                var energy = 0.0
                for (i in 0 until count) energy += buffer[i].toDouble() * buffer[i].toDouble()
                val rms = sqrt(energy / count)
                val now = System.currentTimeMillis()
                if (!segmentActive && rms < speechThresholdRms) noiseFloorRms = (noiseFloorRms * 0.94) + (rms * 0.06)
                val activeThreshold = maxOf(speechThresholdRms, noiseFloorRms * 2.8)
                if (rms >= activeThreshold) {
                    segmentActive = true
                    lastSpeechAt = now
                }
                if (offlineReady) {
                    offlineStt?.accept(buffer, count)?.let { result ->
                        if (result.startsWith("~")) onPartial(result.removePrefix("~")) else onCommitted(result)
                    }
                } else if (socketReady && segmentActive) {
                    val bytes = ByteArray(count * 2)
                    for (i in 0 until count) {
                        val sample = buffer[i].toInt()
                        bytes[i * 2] = (sample and 0xff).toByte()
                        bytes[i * 2 + 1] = ((sample shr 8) and 0xff).toByte()
                    }
                    val payload = JSONObject().put("message_type", "input_audio_chunk")
                        .put("audio_base_64", Base64.encodeToString(bytes, Base64.NO_WRAP))
                        .put("sample_rate", sampleRate).put("commit", false)
                    socket?.send(payload.toString())
                    if (now - lastSpeechAt >= silenceMs) {
                        socket?.send(JSONObject().put("message_type", "commit").toString())
                        segmentActive = false
                    }
                }
            }
        } catch (e: Exception) {
            if (running) onStatus("MIC CAPTURE ERROR: ${e.message ?: "unknown"}")
        } finally {
            try { audio.stop() } catch (_: Exception) {}
            audio.release()
            if (recorder === audio) recorder = null
        }
    }

    private fun cleanTranscript(raw: String): String = SttTranscriptCleaner.clean(raw)

    fun stop() {
        running = false
        socketReady = false
        offlineReady = false
        offlineStt?.close()
        offlineStt = null
        try { recorder?.stop() } catch (_: Exception) {}
        socket?.close(1000, "Vyom voice stopped")
        socket = null
        client.dispatcher.cancelAll()
        client.connectionPool.evictAll()
    }
}
