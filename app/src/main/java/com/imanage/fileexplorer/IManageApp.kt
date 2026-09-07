package com.imanage.fileexplorer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.imanage.fileexplorer.data.local.AppDatabase
import com.imanage.fileexplorer.data.repository.FileSystemRepository
import com.imanage.fileexplorer.data.repository.StorageAnalyzerRepository
import com.imanage.fileexplorer.data.repository.TrashRepository
import com.imanage.fileexplorer.data.repository.VaultRepository

class IManageApp : Application(), ImageLoaderFactory {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val fileSystemRepository: FileSystemRepository by lazy { FileSystemRepository(this) }
    val vaultRepository: VaultRepository by lazy { VaultRepository(this, database.vaultDao()) }
    val trashRepository: TrashRepository by lazy { TrashRepository(this, database.trashDao()) }
    val storageAnalyzerRepository: StorageAnalyzerRepository by lazy { StorageAnalyzerRepository() }
    val tagRepository: com.imanage.fileexplorer.data.repository.TagRepository by lazy { com.imanage.fileexplorer.data.repository.TagRepository(database.tagDao()) }
    val bookmarkRepository: com.imanage.fileexplorer.data.repository.BookmarkRepository by lazy { com.imanage.fileexplorer.data.repository.BookmarkRepository(database.bookmarkDao()) }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Clear any orphaned temp decrypted preview files on startup
        vaultRepository.clearTempDecryptedFiles()
    }

    companion object {
        lateinit var instance: IManageApp
            private set
    }
}
