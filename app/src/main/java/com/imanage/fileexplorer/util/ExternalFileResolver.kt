package com.imanage.fileexplorer.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ExternalFileResolver {

    /**
     * Resolves a file Uri (content:// or file://) from an external intent to a readable File.
     * Tries direct filesystem access first to avoid redundant copies, and falls back to
     * streaming the URI into a temporary cache file if direct access is restricted.
     */
    fun resolveFile(context: Context, uri: Uri, mimeType: String? = null): File? {
        // 1. Check direct file:// scheme
        if ("file".equals(uri.scheme, ignoreCase = true)) {
            val path = uri.path?.let { Uri.decode(it) }
            if (!path.isNullOrEmpty()) {
                try {
                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        return file
                    }
                } catch (_: Exception) {}
            }
        }

        // 2. If content:// scheme, attempt direct resolution
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            val directFile = resolveContentUriToDirectFile(context, uri)
            if (directFile != null) {
                return directFile
            }
        }

        // 3. Fallback: stream content to temporary cache
        return streamUriToCache(context, uri, mimeType)
    }

    private fun resolveContentUriToDirectFile(context: Context, uri: Uri): File? {
        try {
            // A. DocumentsContract (ExternalStorageProvider)
            if (DocumentsContract.isDocumentUri(context, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val authority = uri.authority

                if ("com.android.externalstorage.documents".equals(authority, ignoreCase = true)) {
                    val parts = docId.split(":")
                    if (parts.isNotEmpty()) {
                        val type = parts[0]
                        val relPath = if (parts.size > 1) parts[1] else ""
                        val targetFile = if ("primary".equals(type, ignoreCase = true)) {
                            File(Environment.getExternalStorageDirectory(), relPath)
                        } else {
                            File("/storage/$type/$relPath")
                        }
                        if (targetFile.exists() && targetFile.canRead()) {
                            return targetFile
                        }
                    }
                } else if ("com.android.providers.media.documents".equals(authority, ignoreCase = true)) {
                    val parts = docId.split(":")
                    if (parts.size >= 2) {
                        val mediaType = parts[0]
                        val id = parts[1]
                        val contentUri = when (mediaType) {
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                            else -> MediaStore.Files.getContentUri("external")
                        }
                        queryDataColumn(context, contentUri, "_id=?", arrayOf(id))?.let { return it }
                    }
                }
            }

            // B. Direct MediaStore / ContentProvider _data query
            queryDataColumn(context, uri, null, null)?.let { return it }

            // C. Direct check on uri.path
            uri.path?.let { rawPath ->
                val decoded = Uri.decode(rawPath)
                val f = File(decoded)
                if (f.exists() && f.canRead()) {
                    return f
                }
            }
        } catch (_: Exception) {}

        return null
    }

    private fun queryDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): File? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    if (idx != -1) {
                        val path = cursor.getString(idx)
                        if (!path.isNullOrEmpty()) {
                            val file = File(path)
                            if (file.exists() && file.canRead()) {
                                return file
                            }
                        }
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun streamUriToCache(context: Context, uri: Uri, mimeType: String?): File? {
        return try {
            var displayName: String? = null
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) {
                            displayName = cursor.getString(idx)
                        }
                    }
                }
            } catch (_: Exception) {}

            if (displayName.isNullOrBlank()) {
                displayName = uri.lastPathSegment?.let { Uri.decode(it) }
            }
            if (displayName.isNullOrBlank()) {
                displayName = "opened_${System.currentTimeMillis()}"
            }

            // Ensure valid filename characters
            displayName = displayName!!.replace("[\\\\/:*?\"<>|]".toRegex(), "_")

            // Infer extension if absent
            if (!displayName.contains(".")) {
                val resolvedMime = mimeType ?: context.contentResolver.getType(uri)
                if (!resolvedMime.isNullOrBlank()) {
                    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(resolvedMime)
                    if (!ext.isNullOrBlank()) {
                        displayName = "$displayName.$ext"
                    }
                }
            }

            val baseCacheDir = File(context.cacheDir, "external_opened")
            if (!baseCacheDir.exists()) {
                baseCacheDir.mkdirs()
            }
            cleanOldCacheFiles(baseCacheDir)

            val sessionDir = File(baseCacheDir, UUID.randomUUID().toString())
            sessionDir.mkdirs()

            val targetFile = File(sessionDir, displayName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                targetFile
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun cleanOldCacheFiles(dir: File) {
        try {
            val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            dir.listFiles()?.forEach { file ->
                if (file.lastModified() < oneDayAgo) {
                    file.deleteRecursively()
                }
            }
        } catch (_: Exception) {}
    }
}
