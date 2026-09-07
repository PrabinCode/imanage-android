package com.imanage.fileexplorer.ui.screens.vault

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanage.fileexplorer.data.local.entity.VaultEntity
import com.imanage.fileexplorer.data.repository.VaultRepository
import com.imanage.fileexplorer.data.service.VaultProgressState
import com.imanage.fileexplorer.data.service.VaultService
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VaultUiState(
    val isUnlocked: Boolean = false,
    val items: List<VaultEntity> = emptyList(),
    val activeTask: VaultProgressState? = null,
    val isLoading: Boolean = false,
    val previewFile: File? = null,
    val message: String? = null
)

class VaultViewModel(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        observeVaultItems()
        observeActiveTask()
    }

    private fun observeVaultItems() {
        viewModelScope.launch {
            vaultRepository.vaultItems.collect { items ->
                _uiState.value = _uiState.value.copy(items = items)
            }
        }
    }

    private fun observeActiveTask() {
        viewModelScope.launch {
            VaultService.currentProgress.collect { progress ->
                _uiState.value = _uiState.value.copy(activeTask = progress)
            }
        }
    }

    fun setUnlocked(unlocked: Boolean) {
        _uiState.value = _uiState.value.copy(isUnlocked = unlocked)
    }

    fun importUris(context: Context, uris: List<Uri>) {
        VaultService.startImport(context, uris)
        _uiState.value = _uiState.value.copy(
            message = "Securing ${uris.size} item(s) into vault..."
        )
    }

    fun restoreItem(context: Context, item: VaultEntity) {
        VaultService.startRestore(context, listOf(item.id))
        _uiState.value = _uiState.value.copy(
            message = "Restoring ${item.originalFileName}..."
        )
    }

    fun previewItem(item: VaultEntity, onReady: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = vaultRepository.decryptForPreview(item)
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (result.isSuccess) {
                val file = result.getOrNull()
                if (file != null) {
                    onReady(file)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    message = "Preview failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun deleteItem(item: VaultEntity) {
        viewModelScope.launch {
            vaultRepository.deleteVaultItem(item)
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
