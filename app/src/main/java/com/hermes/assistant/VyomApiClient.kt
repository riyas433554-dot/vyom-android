package com.hermes.assistant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class VyomSecureStore(context: Context) {
    private val prefs = context.getSharedPreferences("vyom_secure", Context.MODE_PRIVATE)
    private val alias = "vyom_session_key"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    fun put(name: String, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val packed = cipher.iv + encrypted
        prefs.edit().putString(name, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    fun get(name: String): String? {
        val packed = prefs.getString(name, null) ?: return null
        return try {
            val bytes = Base64.decode(packed, Base64.NO_WRAP)
            if (bytes.size < 13) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        } catch (_: Exception) {
            prefs.edit().remove(name).apply()
            null
        }
    }

    fun clear(name: String) {
        prefs.edit().remove(name).apply()
    }
}

internal class VyomApiClient(context: Context) {
    private val store = VyomSecureStore(context.applicationContext)
    private val baseUrl = BuildConfig.HERMES_BASE_URL

    fun hasPairingCode(): Boolean = !store.get("pairing_code").isNullOrBlank()

    // Reuse the saved bearer token on reconnect; do not call a session-validation endpoint.
    fun reconnect(): String {
        return store.get("access_token")
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Session missing. Pair Vyom again.")
    }

    fun pair(pairingCode: String): String {
        val response = request(
            "/api/android/session",
            JSONObject().put("pairingCode", pairingCode).put("deviceName", "Vyom Android"),
            null
        )
        val token = response.optString("accessToken")
        if (token.isBlank()) throw IllegalStateException("Server did not return a session token")
        store.put("pairing_code", pairingCode)
        store.put("access_token", token)
        return token
    }

    fun chat(message: String): String {
        var token = store.get("access_token")
        if (token.isNullOrBlank()) {
            val code = store.get("pairing_code")
                ?: throw IllegalStateException("Pair Vyom once to connect to Hermes")
            token = pair(code)
        }
        try {
            return request("/api/android/chat", JSONObject().put("message", message), token)
                .optString("reply", "No reply received")
        } catch (error: UnauthorizedSessionException) {
            val code = store.get("pairing_code")
                ?: throw IllegalStateException("Session expired. Pair Vyom again.")
            token = pair(code)
            return request("/api/android/chat", JSONObject().put("message", message), token)
                .optString("reply", "No reply received")
        }
    }

    private fun request(path: String, body: JSONObject, token: String?): JSONObject {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            if (token != null) setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_UNAUTHORIZED) throw UnauthorizedSessionException()
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = try { JSONObject(text) } catch (_: Exception) { JSONObject() }
            if (status !in 200..299) {
                throw IllegalStateException(json.optString("error").ifBlank { "Hermes API error ($status)" })
            }
            return json
        } finally {
            connection.disconnect()
        }
    }

    private class UnauthorizedSessionException : Exception()
}
