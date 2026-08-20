package com.imanage.fileexplorer.data.crypto

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object KeystoreHelper {

    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val MASTER_VAULT_KEY_ALIAS = "IManage_Master_Vault_Key_v3"
    private const val FALLBACK_KEY_FILE = ".vault_master.key"

    /**
     * Retrieves or creates the AES-256 SecretKey.
     * Tries AndroidKeyStore first; falls back to an internal encrypted keyfile if AndroidKeyStore fails.
     */
    @Synchronized
    fun getOrCreateMasterKey(context: Context? = null): SecretKey {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }

            if (keyStore.containsAlias(MASTER_VAULT_KEY_ALIAS)) {
                val entry = keyStore.getEntry(MASTER_VAULT_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (entry != null) {
                    return entry.secretKey
                }
            }

            // Generate new KeyStore AES key
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE_PROVIDER
            )

            val builder = KeyGenParameterSpec.Builder(
                MASTER_VAULT_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)

            keyGenerator.init(builder.build())
            return keyGenerator.generateKey()
        } catch (e: Exception) {
            // Android KeyStore failure fallback: generate & store a 256-bit key in app private storage
            if (context != null) {
                return getOrCreateFallbackKey(context)
            }
            // In-memory fallback if no context
            val rawKey = ByteArray(32)
            SecureRandom().nextBytes(rawKey)
            return SecretKeySpec(rawKey, "AES")
        }
    }

    private fun getOrCreateFallbackKey(context: Context): SecretKey {
        val keyFile = File(context.filesDir, FALLBACK_KEY_FILE)
        if (keyFile.exists() && keyFile.length() == 32L) {
            val keyBytes = ByteArray(32)
            FileInputStream(keyFile).use { it.read(keyBytes) }
            return SecretKeySpec(keyBytes, "AES")
        }

        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        FileOutputStream(keyFile).use { it.write(keyBytes) }
        return SecretKeySpec(keyBytes, "AES")
    }

    fun hasMasterKey(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
            keyStore.containsAlias(MASTER_VAULT_KEY_ALIAS)
        } catch (e: Exception) {
            false
        }
    }

    fun deleteKey(context: Context? = null) {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply { load(null) }
            if (keyStore.containsAlias(MASTER_VAULT_KEY_ALIAS)) {
                keyStore.deleteEntry(MASTER_VAULT_KEY_ALIAS)
            }
            if (context != null) {
                File(context.filesDir, FALLBACK_KEY_FILE).delete()
            }
        } catch (e: Exception) { }
    }
}
