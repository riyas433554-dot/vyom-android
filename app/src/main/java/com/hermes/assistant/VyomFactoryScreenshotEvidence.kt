package com.hermes.assistant

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

/** Strict image-only intake. Received bytes are copied to app-private storage and never uploaded here. */
internal object VyomFactoryScreenshotEvidencePolicy {
    const val MAX_BYTES = 10 * 1024 * 1024

    fun extensionFor(mimeType: String, bytes: ByteArray): String {
        require(bytes.isNotEmpty() && bytes.size <= MAX_BYTES) { "Screenshot must be 1 byte–10 MiB" }
        return when (mimeType.lowercase()) {
            "image/png" -> {
                val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
                require(bytes.size >= signature.size && signature.indices.all { bytes[it] == signature[it] }) {
                    "Image bytes do not match PNG"
                }
                "png"
            }
            "image/jpeg", "image/jpg" -> {
                require(bytes.size >= 3 && bytes[0] == 0xFF.toByte() &&
                    bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
                    "Image bytes do not match JPEG"
                }
                "jpg"
            }
            "image/webp" -> {
                require(bytes.size >= 12 &&
                    String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                    String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP") {
                    "Image bytes do not match WebP"
                }
                "webp"
            }
            else -> throw IllegalArgumentException("Only PNG, JPEG and WebP screenshots are accepted")
        }
    }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
}

internal class VyomFactoryScreenshotEvidenceStore(private val root: File) {
    data class Received(val file: File, val mimeType: String, val sha256: String, val sizeBytes: Int)

    fun receive(input: InputStream, mimeType: String): Received {
        val bytes = input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                total += read
                require(total <= VyomFactoryScreenshotEvidencePolicy.MAX_BYTES) {
                    "Screenshot exceeds the 10 MiB intake limit"
                }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        }
        val extension = VyomFactoryScreenshotEvidencePolicy.extensionFor(mimeType, bytes)
        val canonicalRoot = root.canonicalFile
        if (!canonicalRoot.exists()) check(canonicalRoot.mkdirs()) { "Could not create app-private screenshot storage" }
        val file = File(canonicalRoot, "${UUID.randomUUID()}.$extension").canonicalFile
        check(file.parentFile == canonicalRoot) { "Screenshot destination escaped app-private storage" }
        FileOutputStream(file).use { output ->
            output.write(bytes)
            output.flush()
            output.fd.sync()
        }
        val stored = file.readBytes()
        val hash = VyomFactoryScreenshotEvidencePolicy.sha256(stored)
        check(stored.contentEquals(bytes) && hash == VyomFactoryScreenshotEvidencePolicy.sha256(bytes)) {
            file.delete()
            "Screenshot storage verification failed"
        }
        return Received(file, mimeType.lowercase(), hash, stored.size)
    }
}
