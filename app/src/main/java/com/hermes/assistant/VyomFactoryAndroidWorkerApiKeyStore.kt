package com.hermes.assistant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** Encrypts the owner-entered provider key with an AES key held by Android Keystore. */
internal class VyomFactoryAndroidWorkerApiKeyStore(
    context: Context,
    private val providerId: String = NvidiaNemotronWorkerModelProvider.PROVIDER_ID
) : VyomFactoryWorkerApiKeyStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val keyIv get() = "${KEY_IV_PREFIX}_$providerId"
    private val keyCiphertext get() = "${KEY_CIPHERTEXT_PREFIX}_$providerId"
    private val keyAlias get() = "${KEY_ALIAS_PREFIX}_$providerId"

    init {
        require(providerId in SUPPORTED_PROVIDER_IDS) { "Unsupported worker credential provider" }
    }

    @Synchronized
    override fun saveKey(value: CharArray) {
        require(value.isNotEmpty() && value.size <= MAX_KEY_CHARS) { "Worker provider key length is invalid" }
        val encoded = Charsets.UTF_8.newEncoder().encode(CharBuffer.wrap(value))
        val bytes = ByteArray(encoded.remaining()).also { encoded.get(it) }
        var encrypted: ByteArray? = null
        var iv: ByteArray? = null
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            encrypted = cipher.doFinal(bytes)
            iv = cipher.iv
            val editor = prefs.edit()
                .putString(keyIv, Base64.encodeToString(iv, Base64.NO_WRAP))
                .putString(keyCiphertext, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            check(editor.commit()) { "Worker provider key could not be stored" }
        } catch (_: Exception) {
            throw IllegalStateException("Worker provider key could not be encrypted")
        } finally {
            encrypted?.fill(0)
            iv?.fill(0)
            bytes.fill(0)
            if (encoded.hasArray()) encoded.array().fill(0)
        }
    }

    @Synchronized
    override fun readKey(): CharArray? {
        val ivValue = prefs.getString(keyIv, null) ?: return null
        val ciphertextValue = prefs.getString(keyCiphertext, null) ?: return null
        val iv = Base64.decode(ivValue, Base64.NO_WRAP)
        val ciphertext = Base64.decode(ciphertextValue, Base64.NO_WRAP)
        val plaintext = try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), javax.crypto.spec.GCMParameterSpec(128, iv))
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            throw IllegalStateException("Worker provider key could not be decrypted")
        } finally {
            ciphertext.fill(0)
        }
        var decoded: CharBuffer? = null
        return try {
            decoded = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(plaintext))
            CharArray(decoded.remaining()).also { decoded.get(it) }
        } catch (_: Exception) {
            throw IllegalStateException("Worker provider key encoding is invalid")
        } finally {
            decoded?.takeIf { it.hasArray() }?.array()?.fill('\u0000')
            plaintext.fill(0)
            iv.fill(0)
        }
    }

    @Synchronized
    override fun clear() {
        check(prefs.edit().remove(keyIv).remove(keyCiphertext).commit()) {
            "Worker provider key could not be cleared"
        }
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(keyAlias)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (store.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val PREFS = "vyom_factory_worker_api_key"
        private const val KEY_IV_PREFIX = "encrypted_key_iv"
        private const val KEY_CIPHERTEXT_PREFIX = "encrypted_key_ciphertext"
        private const val KEY_ALIAS_PREFIX = "vyom_factory_worker_provider_aes"
        private val SUPPORTED_PROVIDER_IDS = setOf(
            NvidiaNemotronWorkerModelProvider.PROVIDER_ID,
            GoogleGemmaWorkerModelProvider.PROVIDER_ID
        )
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val MAX_KEY_CHARS = 500
    }
}
