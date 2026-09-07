package com.imanage.fileexplorer.ui.screens.home

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanage.fileexplorer.IManageApp
import com.imanage.fileexplorer.data.crypto.ShredderEngine
import com.imanage.fileexplorer.data.local.entity.BookmarkEntity
import com.imanage.fileexplorer.data.model.FileItem
import com.imanage.fileexplorer.data.model.FileType
import com.imanage.fileexplorer.data.model.StorageVolumeInfo
import com.imanage.fileexplorer.data.repository.BookmarkRepository
import com.imanage.fileexplorer.data.repository.FileSystemRepository
import com.imanage.fileexplorer.data.repository.TrashRepository
import com.imanage.fileexplorer.data.repository.VaultRepository
import com.imanage.fileexplorer.data.service.VaultService
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val storageVolumes: List<StorageVolumeInfo> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val recentFiles: List<FileItem> = emptyList(),
    val isLoading: Boolean = true,
    val permissionGranted: Boolean = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    },
    val toastMessage: String? = null
)

class HomeViewModel(
    private val fileSystemRepository: FileSystemRepository,
    private val trashRepository: TrashRepository,
    private val vaultRepository: VaultRepository,
    private val bookmarkRepository: BookmarkRepository = IManageApp.instance.bookmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bookmarkRepository.getAllBookmarks().collectLatest { bms ->
                _uiState.value = _uiState.value.copy(bookmarks = bms)
            }
        }
        viewModelScope.launch {
            VaultService.lastCompletedTimestamp.collectLatest { ts ->
                if (ts > 0L) {
                    loadData()
                }
            }
        }
    }

    fun loadData(): Job = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            withContext(Dispatchers.IO) {
                val isStorageManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    true
                }

                val volumes = fileSystemRepository.getStorageVolumes()

                val recents = mutableListOf<FileItem>()
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

                fun collectRecent(dir: File) {
                    try {
                        if (dir.exists()) {
                            dir.walkTopDown().maxDepth(2).filter { it.isFile && !it.name.startsWith(".") }.forEach {
                                recents.add(FileItem.fromFile(it))
                            }
                        }
                    } catch (_: Exception) {}
                }

                collectRecent(downloadDir)
                collectRecent(dcimDir)
                collectRecent(docsDir)

                val sortedRecents = recents.distinctBy { it.path }.sortedByDescending { it.lastModified }.take(20)

                _uiState.value = _uiState.value.copy(
                    storageVolumes = volumes,
                    recentFiles = sortedRecents,
                    permissionGranted = isStorageManager
                )
            }
        } catch (_: Exception) {
        } finally {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun deleteFile(item: FileItem, shred: Boolean) {
        viewModelScope.launch {
            if (shred) {
                ShredderEngine.shred(item.file, passes = 3)
            } else {
                trashRepository.moveToTrash(item.file)
            }
            _uiState.value = _uiState.value.copy(
                toastMessage = if (shred) "Permanently shredded ${item.name}" else "Moved ${item.name} to Trash"
            )
            loadData()
        }
    }

    fun moveToVault(item: FileItem) {
        if (vaultRepository.isPathBusy(item.file.absolutePath)) {
            _uiState.value = _uiState.value.copy(toastMessage = "File is already being moved to Safe Vault")
            return
        }

        VaultService.startEncrypt(
            context = IManageApp.instance,
            paths = listOf(item.file.absolutePath),
            shredOriginal = true
        )
        _uiState.value = _uiState.value.copy(
            toastMessage = "Securing ${item.name} in Safe Vault... (Check notification)"
        )
        loadData()
    }

    fun renameFile(item: FileItem, newName: String) {
        viewModelScope.launch {
            val dest = File(item.file.parentFile, newName)
            fileSystemRepository.moveFile(item.file, dest)
            loadData()
        }
    }

    fun zipFile(item: FileItem) {
        viewModelScope.launch {
            val destZip = File(item.file.parentFile ?: Environment.getExternalStorageDirectory(), "${item.name}.zip")
            fileSystemRepository.zipFiles(listOf(item.file), destZip)
            loadData()
        }
    }

    fun removeBookmark(path: String) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(path)
            _uiState.value = _uiState.value.copy(toastMessage = "Bookmark removed")
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
