package com.hermes.assistant

import android.content.Context
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * On-device English Vosk fallback. The small model is downloaded once on first use,
 * extracted into app-private storage, and then reused offline. No audio leaves the device.
 */
internal class VoskOfflineStt(private val context: Context) : AutoCloseable {
    private var model: Model? = null
    private var recognizer: Recognizer? = null

    fun prepare(onStatus: (String) -> Unit): Boolean {
        return try {
            val root = File(context.filesDir, "vosk-model-small-en-us-0.15")
            if (!File(root, "am").exists()) {
                onStatus("DOWNLOADING OFFLINE ENGLISH MODEL")
                val zip = File(context.cacheDir, "vosk-model.zip")
                val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 60000
                    instanceFollowRedirects = true
                }
                try {
                    connection.inputStream.use { input -> zip.outputStream().use { input.copyTo(it) } }
                } finally { connection.disconnect() }
                ZipInputStream(zip.inputStream().buffered()).use { input ->
                    var entry = input.nextEntry
                    while (entry != null) {
                        val target = File(context.filesDir, entry.name).canonicalFile
                        val base = context.filesDir.canonicalFile
                        require(target.path.startsWith(base.path + File.separator) || target == base) { "Invalid model archive path" }
                        if (entry.isDirectory) target.mkdirs() else {
                            target.parentFile?.mkdirs()
                            target.outputStream().buffered().use { input.copyTo(it) }
                        }
                        input.closeEntry()
                        entry = input.nextEntry
                    }
                }
                zip.delete()
            }
            model = Model(root.absolutePath)
            recognizer = Recognizer(model, 16000.0f)
            true
        } catch (e: Exception) {
            onStatus("OFFLINE STT UNAVAILABLE: ${e.message ?: "model setup failed"}")
            close()
            false
        }
    }

    @Synchronized fun accept(samples: ShortArray, count: Int): String? {
        val r = recognizer ?: return null
        if (count <= 0) return null
        return try {
            if (r.acceptWaveForm(samples, count)) {
                JSONObject(r.result).optString("text").trim().takeIf { it.isNotBlank() }
            } else {
                JSONObject(r.partialResult).optString("partial").trim().takeIf { it.isNotBlank() }?.let { "~$it" }
            }
        } catch (_: Exception) { null }
    }

    @Synchronized override fun close() {
        try { recognizer?.close() } catch (_: Exception) {}
        recognizer = null
        try { model?.close() } catch (_: Exception) {}
        model = null
    }

    companion object {
        private const val MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
    }
}
