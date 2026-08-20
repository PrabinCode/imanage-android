package com.imanage.fileexplorer.ui.screens.home

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanage.fileexplorer.data.crypto.ShredderEngine
import com.imanage.fileexplorer.data.model.FileItem
import com.imanage.fileexplorer.data.model.FileType
import com.imanage.fileexplorer.data.model.StorageVolumeInfo
import com.imanage.fileexplorer.data.repository.FileSystemRepository
import com.imanage.fileexplorer.data.repository.TrashRepository
import com.imanage.fileexplorer.data.repository.VaultRepository
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val storageVolumes: List<StorageVolumeInfo> = emptyList(),
    val recentFiles: List<FileItem> = emptyList(),
    val isLoading: Boolean = true,
    val permissionGranted: Boolean = false,
    val toastMessage: String? = null
)

class HomeViewModel(
    private val fileSystemRepository: FileSystemRepository,
    private val trashRepository: TrashRepository,
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

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
                if (dir.exists()) {
                    dir.walkTopDown().maxDepth(2).filter { it.isFile && !it.name.startsWith(".") }.forEach {
                        recents.add(FileItem.fromFile(it))
                    }
                }
            }

            collectRecent(downloadDir)
            collectRecent(dcimDir)
            collectRecent(docsDir)

            val sortedRecents = recents.distinctBy { it.path }.sortedByDescending { it.lastModified }.take(20)

            _uiState.value = _uiState.value.copy(
                storageVolumes = volumes,
                recentFiles = sortedRecents,
                isLoading = false,
                permissionGranted = isStorageManager
            )
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
        viewModelScope.launch {
            val result = vaultRepository.moveToVault(item.file, shredOriginal = true)
            _uiState.value = _uiState.value.copy(
                toastMessage = if (result.isSuccess) "Encrypted and moved ${item.name} to Safe Vault" else "Vault error: ${result.exceptionOrNull()?.message}"
            )
            loadData()
        }
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

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
