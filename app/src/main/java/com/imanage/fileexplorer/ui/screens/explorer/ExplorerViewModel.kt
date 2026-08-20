package com.imanage.fileexplorer.ui.screens.explorer

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanage.fileexplorer.data.model.*
import com.imanage.fileexplorer.data.repository.FileSystemRepository
import com.imanage.fileexplorer.data.repository.TrashRepository
import com.imanage.fileexplorer.data.repository.VaultRepository
import com.imanage.fileexplorer.data.crypto.ShredderEngine
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ClipboardState(
    val items: List<FileItem> = emptyList(),
    val isCut: Boolean = false
)

data class ExplorerUiState(
    val currentPath: String = Environment.getExternalStorageDirectory().absolutePath,
    val title: String = "Internal Storage",
    val isCategoryMode: Boolean = false,
    val currentCategory: FileType? = null,
    val files: List<FileItem> = emptyList(),
    val selectedFiles: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val sortOption: SortOption = SortOption(showHiddenFiles = true),
    val viewMode: ViewMode = ViewMode.LIST,
    val clipboard: ClipboardState? = null,
    val operationProgress: OperationProgress? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val toastMessage: String? = null
)

class ExplorerViewModel(
    private val fileSystemRepository: FileSystemRepository,
    private val trashRepository: TrashRepository,
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    fun navigateTo(path: String, title: String? = null) {
        val cleanTitle = title ?: File(path).name.ifEmpty { if (path == "/") "System Root (/)" else "Storage" }
        _uiState.value = _uiState.value.copy(
            currentPath = path,
            title = cleanTitle,
            isCategoryMode = false,
            currentCategory = null,
            selectedFiles = emptySet(),
            isSelectionMode = false
        )
        loadDirectory(path)
    }

    fun navigateToCategory(categoryType: FileType) {
        _uiState.value = _uiState.value.copy(
            title = categoryType.displayName,
            isCategoryMode = true,
            currentCategory = categoryType,
            selectedFiles = emptySet(),
            isSelectionMode = false
        )
        loadCategory(categoryType)
    }

    fun loadCurrent() {
        if (_uiState.value.isCategoryMode && _uiState.value.currentCategory != null) {
            loadCategory(_uiState.value.currentCategory!!)
        } else {
            loadDirectory(_uiState.value.currentPath)
        }
    }

    fun loadDirectory(path: String = _uiState.value.currentPath) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val items = fileSystemRepository.getDirectoryContents(path, _uiState.value.sortOption)
            _uiState.value = _uiState.value.copy(
                files = items,
                isLoading = false
            )
        }
    }

    fun loadCategory(categoryType: FileType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val items = fileSystemRepository.getCategoryFiles(categoryType)
            _uiState.value = _uiState.value.copy(
                files = items,
                isLoading = false
            )
        }
    }

    fun toggleSelection(path: String) {
        val current = _uiState.value.selectedFiles.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }

        _uiState.value = _uiState.value.copy(
            selectedFiles = current,
            isSelectionMode = current.isNotEmpty()
        )
    }

    fun selectAll() {
        val allPaths = _uiState.value.files.map { it.path }.toSet()
        _uiState.value = _uiState.value.copy(
            selectedFiles = allPaths,
            isSelectionMode = true
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedFiles = emptySet(),
            isSelectionMode = false
        )
    }

    fun updateSortOption(newOption: SortOption) {
        _uiState.value = _uiState.value.copy(sortOption = newOption)
        loadCurrent()
    }

    fun toggleViewMode() {
        val next = if (_uiState.value.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        _uiState.value = _uiState.value.copy(viewMode = next)
    }

    fun copySelected() {
        val selected = _uiState.value.files.filter { _uiState.value.selectedFiles.contains(it.path) }
        _uiState.value = _uiState.value.copy(
            clipboard = ClipboardState(items = selected, isCut = false),
            selectedFiles = emptySet(),
            isSelectionMode = false
        )
    }

    fun cutSelected() {
        val selected = _uiState.value.files.filter { _uiState.value.selectedFiles.contains(it.path) }
        _uiState.value = _uiState.value.copy(
            clipboard = ClipboardState(items = selected, isCut = true),
            selectedFiles = emptySet(),
            isSelectionMode = false
        )
    }

    fun pasteClipboard() {
        val clip = _uiState.value.clipboard ?: return
        viewModelScope.launch {
            val targetDir = File(_uiState.value.currentPath)
            for (item in clip.items) {
                val destFile = File(targetDir, item.name)
                if (clip.isCut) {
                    fileSystemRepository.moveFile(item.file, destFile)
                } else {
                    fileSystemRepository.copyFile(item.file, destFile)
                }
            }
            _uiState.value = _uiState.value.copy(clipboard = null)
            loadCurrent()
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = fileSystemRepository.createFolder(_uiState.value.currentPath, name)
            if (result.isSuccess) {
                loadCurrent()
            }
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            val result = fileSystemRepository.createFile(_uiState.value.currentPath, name)
            if (result.isSuccess) {
                loadCurrent()
            }
        }
    }

    fun renameFile(item: FileItem, newName: String) {
        viewModelScope.launch {
            val dest = File(item.file.parentFile, newName)
            val result = fileSystemRepository.moveFile(item.file, dest)
            if (result.isSuccess) {
                loadCurrent()
            }
        }
    }

    fun deleteItems(items: List<FileItem>, shred: Boolean) {
        viewModelScope.launch {
            for (item in items) {
                if (shred) {
                    ShredderEngine.shred(item.file, passes = 3)
                } else {
                    trashRepository.moveToTrash(item.file)
                }
            }
            clearSelection()
            loadCurrent()
            _uiState.value = _uiState.value.copy(
                toastMessage = if (shred) "Permanently shredded ${items.size} item(s)" else "Moved ${items.size} item(s) to Trash"
            )
        }
    }

    fun moveItemsToVault(items: List<FileItem>) {
        viewModelScope.launch {
            var successCount = 0
            var lastError: String? = null
            for (item in items) {
                val result = vaultRepository.moveToVault(item.file, shredOriginal = true)
                if (result.isSuccess) {
                    successCount++
                } else {
                    lastError = result.exceptionOrNull()?.message ?: "Encryption failed"
                }
            }
            clearSelection()
            loadCurrent()
            _uiState.value = _uiState.value.copy(
                toastMessage = if (successCount > 0) "Encrypted and moved $successCount item(s) to Safe Vault" else "Vault error: $lastError"
            )
        }
    }

    fun zipItems(items: List<FileItem>, zipName: String = "Archive_${System.currentTimeMillis()}.zip") {
        viewModelScope.launch {
            val destDir = if (!_uiState.value.isCategoryMode) File(_uiState.value.currentPath) else Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destZip = File(destDir, zipName)
            fileSystemRepository.zipFiles(items.map { it.file }, destZip)
            clearSelection()
            loadCurrent()
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
