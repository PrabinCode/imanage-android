package com.imanage.fileexplorer.data.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.imanage.fileexplorer.data.crypto.ShredderEngine
import com.imanage.fileexplorer.data.crypto.VaultCryptoEngine
import com.imanage.fileexplorer.data.local.dao.VaultDao
import com.imanage.fileexplorer.data.local.entity.VaultEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VaultRepository(
    private val context: Context,
    private val vaultDao: VaultDao
) {
    private val vaultDir: File get() = File(context.filesDir, ".imanage_vault").apply { mkdirs() }
    private val tempDecryptedDir: File get() = File(context.cacheDir, ".vault_temp").apply { mkdirs() }

    private val inFlightPaths = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val inFlightVaultIds = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()

    val vaultItems: Flow<List<VaultEntity>> = vaultDao.getAllVaultItems()

    fun isPathBusy(path: String): Boolean = inFlightPaths.contains(path)

    suspend fun moveToVault(
        originalFile: File,
        shredOriginal: Boolean = true,
        onProgress: (bytesProcessed: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<VaultEntity> = withContext(Dispatchers.IO) {
        val originalPath = originalFile.absolutePath

        // Concurrency guard: prevent duplicate parallel encryption of the same file
        if (!inFlightPaths.add(originalPath)) {
            return@withContext Result.failure(IllegalStateException("File is already being moved to Safe Vault: ${originalFile.name}"))
        }

        try {
            if (!originalFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("File does not exist: $originalPath"))
            }

            // DB deduplication: prevent duplicate rows if already in Safe Vault
            if (vaultDao.isPathInVault(originalPath)) {
                return@withContext Result.failure(IllegalStateException("File is already in Safe Vault: ${originalFile.name}"))
            }

            val isDir = originalFile.isDirectory
            val originalName = originalFile.name
            val encFileName = "${UUID.randomUUID()}.enc"
            val destEncFile = File(vaultDir, encFileName)

            val fileToEncrypt: File
            val totalSize: Long

            if (isDir) {
                val tempZip = File(context.cacheDir, "${UUID.randomUUID()}.zip")
                zipDirectory(originalFile, tempZip)
                fileToEncrypt = tempZip
                totalSize = tempZip.length()
            } else {
                fileToEncrypt = originalFile
                totalSize = originalFile.length()
            }

            val encResult = VaultCryptoEngine.encryptFile(
                sourceFile = fileToEncrypt,
                destFile = destEncFile,
                context = context,
                onProgress = onProgress
            )

            if (isDir && fileToEncrypt.exists()) {
                fileToEncrypt.delete()
            }

            if (encResult.isFailure) {
                return@withContext Result.failure(encResult.exceptionOrNull() ?: Exception("Encryption failed"))
            }

            val ext = originalFile.extension.lowercase()
            val mime = if (isDir) "resource/folder" else (MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*")

            val entity = VaultEntity(
                encryptedFileName = encFileName,
                originalFileName = originalName,
                originalPath = originalPath,
                fileSize = totalSize,
                mimeType = mime,
                isDirectory = isDir
            )

            val id = vaultDao.insertVaultItem(entity)
            val savedEntity = entity.copy(id = id)

            if (shredOriginal) {
                ShredderEngine.quickSecureWipe(originalFile, context = context)
            }

            try {
                MediaScannerConnection.scanFile(context, arrayOf(originalPath), null, null)
            } catch (e: Exception) { }

            Result.success(savedEntity)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            inFlightPaths.remove(originalPath)
        }
    }

    suspend fun importUriToVault(
        uri: Uri,
        onProgress: (bytesProcessed: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<VaultEntity> = withContext(Dispatchers.IO) {
        try {
            var fileName = "Imported_${System.currentTimeMillis()}"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val tempImportFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempImportFile).use { output ->
                    input.copyTo(output)
                }
            }

            val result = moveToVault(tempImportFile, shredOriginal = true, onProgress = onProgress)
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromVault(
        vaultEntity: VaultEntity,
        targetDirectory: File? = null,
        onProgress: (bytesProcessed: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!inFlightVaultIds.add(vaultEntity.id)) {
            return@withContext Result.failure(IllegalStateException("Item is already being restored: ${vaultEntity.originalFileName}"))
        }

        try {
            val encFile = File(vaultDir, vaultEntity.encryptedFileName)
            val originalParent = File(vaultEntity.originalPath).parentFile
            val targetDir = targetDirectory ?: if (originalParent != null && originalParent.exists()) originalParent else File(android.os.Environment.getExternalStorageDirectory(), "Download")
            targetDir.mkdirs()

            if (vaultEntity.isDirectory) {
                val tempZip = File(context.cacheDir, "${UUID.randomUUID()}.zip")
                val decResult = VaultCryptoEngine.decryptFile(encFile, tempZip, context = context, onProgress = onProgress)
                if (decResult.isSuccess) {
                    val destFolder = File(targetDir, vaultEntity.originalFileName)
                    destFolder.mkdirs()
                    unzipToDirectory(tempZip, destFolder)
                    tempZip.delete()
                    encFile.delete()
                    vaultDao.deleteById(vaultEntity.id)
                    MediaScannerConnection.scanFile(context, arrayOf(destFolder.absolutePath), null, null)
                    Result.success(destFolder)
                } else {
                    Result.failure(decResult.exceptionOrNull() ?: Exception("Decryption failed"))
                }
            } else {
                var targetFile = File(targetDir, vaultEntity.originalFileName)
                if (targetFile.exists()) {
                    val nameWithoutExt = targetFile.nameWithoutExtension
                    val ext = targetFile.extension
                    val dotExt = if (ext.isNotEmpty()) ".$ext" else ""
                    targetFile = File(targetDir, "${nameWithoutExt}_restored_${System.currentTimeMillis()}$dotExt")
                }

                val decResult = VaultCryptoEngine.decryptFile(encFile, targetFile, context = context, onProgress = onProgress)
                if (decResult.isSuccess) {
                    encFile.delete()
                    vaultDao.deleteById(vaultEntity.id)
                    MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null, null)
                    Result.success(targetFile)
                } else {
                    Result.failure(decResult.exceptionOrNull() ?: Exception("Decryption failed"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            inFlightVaultIds.remove(vaultEntity.id)
        }
    }

    suspend fun decryptForPreview(vaultEntity: VaultEntity): Result<File> = withContext(Dispatchers.IO) {
        try {
            val encFile = File(vaultDir, vaultEntity.encryptedFileName)
            if (vaultEntity.isDirectory) {
                val tempZip = File(tempDecryptedDir, "${vaultEntity.originalFileName}.zip")
                val result = VaultCryptoEngine.decryptFile(encFile, tempZip, context = context)
                result
            } else {
                val tempFile = File(tempDecryptedDir, vaultEntity.originalFileName)
                val result = VaultCryptoEngine.decryptFile(encFile, tempFile, context = context)
                result
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVaultItem(vaultEntity: VaultEntity): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val encFile = File(vaultDir, vaultEntity.encryptedFileName)
            ShredderEngine.quickSecureWipe(encFile, context = context)
            vaultDao.deleteById(vaultEntity.id)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearTempDecryptedFiles() {
        tempDecryptedDir.deleteRecursively()
        tempDecryptedDir.mkdirs()
    }

    private fun zipDirectory(dir: File, destZip: File) {
        ZipOutputStream(FileOutputStream(destZip)).use { zos ->
            fun addEntry(file: File, baseName: String) {
                if (file.isDirectory) {
                    file.listFiles()?.forEach { child ->
                        addEntry(child, "$baseName/${child.name}")
                    }
                } else {
                    val entry = ZipEntry(baseName)
                    zos.putNextEntry(entry)
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos, 64 * 1024)
                    }
                    zos.closeEntry()
                }
            }
            dir.listFiles()?.forEach { child ->
                addEntry(child, child.name)
            }
        }
    }

    private fun unzipToDirectory(zipFile: File, destDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos, 64 * 1024)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
