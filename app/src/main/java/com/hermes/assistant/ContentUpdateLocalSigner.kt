package com.hermes.assistant

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

/** Device-local non-exportable signer for owner-prompt-generated content packages. */
internal class ContentUpdateLocalSigner(context: Context) {
    private val appContext = context.applicationContext

    fun publicKeyBase64(): String {
        val certificate = ensureKeyStore().getCertificate(KEY_ALIAS)
            ?: throw IllegalStateException("Local content signing certificate is missing")
        return Base64.getEncoder().encodeToString(certificate.publicKey.encoded)
    }

    fun sign(canonicalPayload: ByteArray): ByteArray {
        val key = ensureKeyStore().getKey(KEY_ALIAS, null) as? PrivateKey
            ?: throw IllegalStateException("Local content signing key is unavailable")
        return Signature.getInstance("SHA256withECDSA").run {
            initSign(key)
            update(canonicalPayload)
            sign()
        }
    }

    private fun ensureKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
            generator.initialize(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            generator.generateKeyPair()
        }
        return keyStore
    }

    companion object {
        private const val KEY_ALIAS = "vyom-content-update-local-v1"
    }
}
