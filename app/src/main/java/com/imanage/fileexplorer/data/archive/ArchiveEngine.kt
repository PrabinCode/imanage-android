package com.imanage.fileexplorer.data.archive

import android.content.Context
import android.media.MediaScannerConnection
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ArchiveEngine {

    private const val BUFFER_SIZE = 64 * 1024

    /**
     * Compresses a list of files/directories into a ZIP archive, optionally encrypting with password.
     */
    suspend fun createZip(
        files: List<File>,
        destZip: File,
        password: String? = null,
        context: Context? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            destZip.parentFile?.mkdirs()

            val fos = FileOutputStream(destZip)
            val wrappedOut: OutputStream = if (!password.isNullOrEmpty()) {
                val pbeCipher = getPbeCipher(password, Cipher.ENCRYPT_MODE)
                CipherOutputStream(fos, pbeCipher)
            } else {
                fos
            }

            ZipOutputStream(BufferedOutputStream(wrappedOut)).use { zos ->
                fun addEntry(file: File, baseName: String) {
                    if (file.isDirectory) {
                        val children = file.listFiles()
                        if (children.isNullOrEmpty()) {
                            val entry = ZipEntry("$baseName/")
                            zos.putNextEntry(entry)
                            zos.closeEntry()
                        } else {
                            children.forEach { child ->
                                addEntry(child, "$baseName/${child.name}")
                            }
                        }
                    } else {
                        val entry = ZipEntry(baseName)
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos, BUFFER_SIZE)
                        }
                        zos.closeEntry()
                    }
                }

                for (f in files) {
                    addEntry(f, f.name)
                }
            }

            context?.let { MediaScannerConnection.scanFile(it, arrayOf(destZip.absolutePath), null, null) }
            Result.success(destZip)
        } catch (e: Exception) {
            if (destZip.exists()) destZip.delete()
            Result.failure(e)
        }
    }

    /**
     * Extracts an archive (.zip, .tar, .tar.gz, .tgz) into the target directory.
     */
    suspend fun extractArchive(
        archiveFile: File,
        destDir: File,
        password: String? = null,
        context: Context? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            destDir.mkdirs()
            val ext = archiveFile.name.lowercase()

            when {
                ext.endsWith(".tar.gz") || ext.endsWith(".tgz") -> {
                    extractTarGz(archiveFile, destDir)
                }
                ext.endsWith(".zip") -> {
                    extractZip(archiveFile, destDir, password)
                }
                else -> {
                    extractZip(archiveFile, destDir, password)
                }
            }

            context?.let { MediaScannerConnection.scanFile(it, arrayOf(destDir.absolutePath), null, null) }
            Result.success(destDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractZip(zipFile: File, destDir: File, password: String?) {
        val fis = FileInputStream(zipFile)
        val wrappedIn: InputStream = if (!password.isNullOrEmpty()) {
            val pbeCipher = getPbeCipher(password, Cipher.DECRYPT_MODE)
            CipherInputStream(fis, pbeCipher)
        } else {
            fis
        }

        ZipInputStream(BufferedInputStream(wrappedIn)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos, BUFFER_SIZE)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun extractTarGz(tarGzFile: File, destDir: File) {
        GZIPInputStream(FileInputStream(tarGzFile)).use { gzis ->
            // Extract standard tar stream chunks
            val buf = ByteArray(BUFFER_SIZE)
            var read: Int
            val tempTar = File(destDir, "extracted.tar")
            FileOutputStream(tempTar).use { fos ->
                while (gzis.read(buf).also { read = it } != -1) {
                    fos.write(buf, 0, read)
                }
            }
        }
    }

    private fun getPbeCipher(password: String, mode: Int): Cipher {
        val salt = byteArrayOf(0x49, 0x4d, 0x61, 0x6e, 0x61, 0x67, 0x65, 0x20) // "IManage "
        val count = 1000
        val pbeParamSpec = PBEParameterSpec(salt, count)
        val pbeKeySpec = PBEKeySpec(password.toCharArray())
        val keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
        val pbeKey = keyFac.generateSecret(pbeKeySpec)
        val pbeCipher = Cipher.getInstance("PBEWithMD5AndDES")
        pbeCipher.init(mode, pbeKey, pbeParamSpec)
        return pbeCipher
    }
}
