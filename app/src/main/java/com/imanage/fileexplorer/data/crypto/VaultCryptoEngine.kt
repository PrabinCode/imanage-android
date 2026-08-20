package com.imanage.fileexplorer.data.crypto

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VaultCryptoEngine {

    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
    private const val IV_LENGTH = 16
    private const val BUFFER_SIZE = 64 * 1024

    suspend fun encryptFile(
        sourceFile: File,
        destFile: File,
        context: Context? = null,
        secretKey: SecretKey = KeystoreHelper.getOrCreateMasterKey(context),
        onProgress: (bytesProcessed: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists() || !sourceFile.isFile) {
                return@withContext Result.failure(IllegalArgumentException("Source is not a file: ${sourceFile.absolutePath}"))
            }

            destFile.parentFile?.mkdirs()

            // Initialize cipher for encryption without passing IV so AndroidKeyStore generates it safely
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv ?: return@withContext Result.failure(IllegalStateException("Cipher failed to generate IV"))

            val totalBytes = sourceFile.length().coerceAtLeast(1L)
            var bytesProcessed = 0L

            FileOutputStream(destFile).use { fos ->
                // Write IV to file header first
                fos.write(iv)

                FileInputStream(sourceFile).use { fis ->
                    val inBuffer = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while (fis.read(inBuffer).also { read = it } != -1) {
                        val outBytes = cipher.update(inBuffer, 0, read)
                        if (outBytes != null && outBytes.isNotEmpty()) {
                            fos.write(outBytes)
                        }
                        bytesProcessed += read
                        onProgress(bytesProcessed, totalBytes)
                    }

                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null && finalBytes.isNotEmpty()) {
                        fos.write(finalBytes)
                    }
                }
            }

            Result.success(destFile)
        } catch (e: Exception) {
            if (destFile.exists()) {
                destFile.delete()
            }
            Result.failure(e)
        }
    }

    suspend fun decryptFile(
        encryptedFile: File,
        destFile: File,
        context: Context? = null,
        secretKey: SecretKey = KeystoreHelper.getOrCreateMasterKey(context),
        onProgress: (bytesProcessed: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!encryptedFile.exists() || encryptedFile.length() < IV_LENGTH) {
                return@withContext Result.failure(IllegalArgumentException("Invalid encrypted file: ${encryptedFile.absolutePath}"))
            }

            destFile.parentFile?.mkdirs()

            val totalBytes = (encryptedFile.length() - IV_LENGTH).coerceAtLeast(1L)
            var bytesProcessed = 0L

            FileInputStream(encryptedFile).use { fis ->
                val iv = ByteArray(IV_LENGTH)
                val readIv = fis.read(iv)
                if (readIv != IV_LENGTH) {
                    return@withContext Result.failure(IllegalStateException("Could not read IV header"))
                }

                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

                FileOutputStream(destFile).use { fos ->
                    val inBuffer = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while (fis.read(inBuffer).also { read = it } != -1) {
                        val outBytes = cipher.update(inBuffer, 0, read)
                        if (outBytes != null && outBytes.isNotEmpty()) {
                            fos.write(outBytes)
                        }
                        bytesProcessed += read
                        onProgress(bytesProcessed, totalBytes)
                    }

                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null && finalBytes.isNotEmpty()) {
                        fos.write(finalBytes)
                    }
                }
            }

            Result.success(destFile)
        } catch (e: Exception) {
            if (destFile.exists()) {
                destFile.delete()
            }
            Result.failure(e)
        }
    }
}
