package com.imanage.fileexplorer.ui.screens.explorer

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanage.fileexplorer.IManageApp
import com.imanage.fileexplorer.data.archive.ArchiveEngine
import com.imanage.fileexplorer.data.crypto.ShredderEngine
import com.imanage.fileexplorer.data.model.*
import com.imanage.fileexplorer.data.repository.FileSystemRepository
import com.imanage.fileexplorer.data.repository.TagRepository
import com.imanage.fileexplorer.data.repository.TrashRepository
import com.imanage.fileexplorer.data.repository.VaultRepository
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.imanage.fileexplorer.data.local.entity.BookmarkEntity
import com.imanage.fileexplorer.data.repository.BookmarkRepository

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
    val tagsMap: Map<String, String> = emptyMap(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val storageVolumes: List<StorageVolumeInfo> = emptyList(),
    val isBookmarked: Boolean = false,
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
    private val vaultRepository: VaultRepository,
    private val tagRepository: TagRepository = IManageApp.instance.tagRepository,
    private val bookmarkRepository: BookmarkRepository = IManageApp.instance.bookmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    init {
        val volumes = fileSystemRepository.getStorageVolumes()
        _uiState.value = _uiState.value.copy(storageVolumes = volumes)

        viewModelScope.launch {
            tagRepository.getAllTags().collectLatest { tags ->
                _uiState.value = _uiState.value.copy(
                    tagsMap = tags.associate { it.path to it.colorHex }
                )
            }
        }
        viewModelScope.launch {
            bookmarkRepository.getAllBookmarks().collectLatest { bms ->
                val current = _uiState.value.currentPath
                _uiState.value = _uiState.value.copy(
                    bookmarks = bms,
                    isBookmarked = bms.any { it.path == current }
                )
            }
        }
    }

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

    fun navigateToCategory(type: FileType) {
        loadCategory(type)
    }

    fun loadCategory(type: FileType) {
        _uiState.value = _uiState.value.copy(
            title = type.displayName,
            isCategoryMode = true,
            currentCategory = type,
            selectedFiles = emptySet(),
            isSelectionMode = false,
            isLoading = true,
            errorMessage = null
        )
        viewModelScope.launch {
            try {
                val categoryFiles = fileSystemRepository.getCategoryFiles(type)
                _uiState.value = _uiState.value.copy(
                    files = categoryFiles,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to scan ${type.displayName} files"
                )
            }
        }
    }

    fun loadDirectory(path: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val files = fileSystemRepository.getDirectoryContents(path, _uiState.value.sortOption)
                val bookmarked = _uiState.value.bookmarks.any { it.path == path }
                _uiState.value = _uiState.value.copy(
                    files = files,
                    isBookmarked = bookmarked,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load directory"
                )
            }
        }
    }

    fun loadCurrent() {
        if (_uiState.value.isCategoryMode && _uiState.value.currentCategory != null) {
            loadCategory(_uiState.value.currentCategory!!)
        } else {
            loadDirectory(_uiState.value.currentPath)
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

    fun toggleFileSelection(path: String) {
        toggleSelection(path)
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

    fun setSortOption(option: SortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
        loadCurrent()
    }

    fun updateSortOption(option: SortOption) {
        setSortOption(option)
    }

    fun toggleViewMode() {
        val newMode = if (_uiState.value.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        _uiState.value = _uiState.value.copy(viewMode = newMode)
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = fileSystemRepository.createFolder(_uiState.value.currentPath, name)
            result.onSuccess {
                loadCurrent()
                _uiState.value = _uiState.value.copy(toastMessage = "Folder created: $name")
            }.onFailure {
                _uiState.value = _uiState.value.copy(toastMessage = "Error: ${it.message}")
            }
        }
    }

    fun createFile(name: String) {
        viewModelScope.launch {
            val result = fileSystemRepository.createFile(_uiState.value.currentPath, name)
            result.onSuccess {
                loadCurrent()
                _uiState.value = _uiState.value.copy(toastMessage = "File created: $name")
            }.onFailure {
                _uiState.value = _uiState.value.copy(toastMessage = "Error: ${it.message}")
            }
        }
    }

    fun renameFile(item: FileItem, newName: String) {
        viewModelScope.launch {
            val target = File(item.file.parentFile, newName)
            val result = fileSystemRepository.moveFile(item.file, target)
            result.onSuccess {
                loadCurrent()
                _uiState.value = _uiState.value.copy(toastMessage = "Renamed to $newName")
            }.onFailure {
                _uiState.value = _uiState.value.copy(toastMessage = "Rename failed: ${it.message}")
            }
        }
    }

    fun copyItems(items: List<FileItem>) {
        _uiState.value = _uiState.value.copy(
            clipboard = ClipboardState(items = items, isCut = false),
            selectedFiles = emptySet(),
            isSelectionMode = false,
            toastMessage = "Copied ${items.size} item(s)"
        )
    }

    fun copySelected() {
        val selected = _uiState.value.files.filter { _uiState.value.selectedFiles.contains(it.path) }
        copyItems(selected)
    }

    fun cutItems(items: List<FileItem>) {
        _uiState.value = _uiState.value.copy(
            clipboard = ClipboardState(items = items, isCut = true),
            selectedFiles = emptySet(),
            isSelectionMode = false,
            toastMessage = "Cut ${items.size} item(s)"
        )
    }

    fun cutSelected() {
        val selected = _uiState.value.files.filter { _uiState.value.selectedFiles.contains(it.path) }
        cutItems(selected)
    }

    fun pasteClipboard() {
        pasteItems()
    }

    fun pasteItems() {
        val clip = _uiState.value.clipboard ?: return
        viewModelScope.launch {
            val targetDir = File(_uiState.value.currentPath)
            var errorCount = 0
            for (item in clip.items) {
                val dest = File(targetDir, item.name)
                val result = if (clip.isCut) {
                    fileSystemRepository.moveFile(item.file, dest)
                } else {
                    fileSystemRepository.copyFile(item.file, dest)
                }
                if (result.isFailure) errorCount++
            }

            _uiState.value = _uiState.value.copy(
                clipboard = if (clip.isCut) null else clip,
                toastMessage = if (errorCount == 0) "Pasted successfully" else "$errorCount items failed"
            )
            loadCurrent()
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

    fun zipItems(items: List<FileItem>, zipName: String = "Archive.zip", password: String? = null) {
        viewModelScope.launch {
            val destDir = if (!_uiState.value.isCategoryMode) File(_uiState.value.currentPath) else Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val destZip = File(destDir, zipName)
            val result = ArchiveEngine.createZip(items.map { it.file }, destZip, password)
            clearSelection()
            loadCurrent()
            _uiState.value = _uiState.value.copy(
                toastMessage = if (result.isSuccess) "Created ${destZip.name}" else "ZIP Error: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    fun extractArchive(item: FileItem, password: String? = null) {
        viewModelScope.launch {
            val destDir = File(_uiState.value.currentPath, item.file.nameWithoutExtension)
            val result = ArchiveEngine.extractArchive(item.file, destDir, password)
            loadCurrent()
            _uiState.value = _uiState.value.copy(
                toastMessage = if (result.isSuccess) "Extracted to ${destDir.name}" else "Extraction Error: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    fun assignTag(path: String, colorHex: String, tagName: String) {
        viewModelScope.launch {
            tagRepository.setTag(path, colorHex, tagName)
            _uiState.value = _uiState.value.copy(toastMessage = "Tagged as $tagName")
        }
    }

    fun removeTag(path: String) {
        viewModelScope.launch {
            tagRepository.removeTag(path)
            _uiState.value = _uiState.value.copy(toastMessage = "Tag removed")
        }
    }

    fun toggleBookmarkCurrentPath() {
        val current = _uiState.value.currentPath
        val isCurrentlyBookmarked = _uiState.value.isBookmarked
        val title = _uiState.value.title
        viewModelScope.launch {
            if (isCurrentlyBookmarked) {
                bookmarkRepository.removeBookmark(current)
                _uiState.value = _uiState.value.copy(
                    isBookmarked = false,
                    toastMessage = "Removed from bookmarks"
                )
            } else {
                bookmarkRepository.addBookmark(current, title, isDirectory = true)
                _uiState.value = _uiState.value.copy(
                    isBookmarked = true,
                    toastMessage = "Added to bookmarks"
                )
            }
        }
    }

    fun removeBookmark(path: String) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(path)
            _uiState.value = _uiState.value.copy(
                isBookmarked = if (_uiState.value.currentPath == path) false else _uiState.value.isBookmarked,
                toastMessage = "Bookmark removed"
            )
        }
    }

    fun batchRename(items: List<FileItem>, newNames: List<String>) {
        viewModelScope.launch {
            val (successCount, errors) = fileSystemRepository.batchRename(items, newNames)
            clearSelection()
            loadCurrent()
            val msg = if (errors.isEmpty()) {
                "Renamed $successCount items successfully"
            } else {
                "Renamed $successCount items (${errors.size} failed)"
            }
            _uiState.value = _uiState.value.copy(toastMessage = msg)
        }
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
