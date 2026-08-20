package com.imanage.fileexplorer.ui.screens.vault

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanage.fileexplorer.data.local.entity.VaultEntity
import com.imanage.fileexplorer.data.repository.VaultRepository
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VaultUiState(
    val isUnlocked: Boolean = false,
    val items: List<VaultEntity> = emptyList(),
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
    }

    private fun observeVaultItems() {
        viewModelScope.launch {
            vaultRepository.vaultItems.collect { items ->
                _uiState.value = _uiState.value.copy(items = items)
            }
        }
    }

    fun setUnlocked(unlocked: Boolean) {
        _uiState.value = _uiState.value.copy(isUnlocked = unlocked)
    }

    fun importUris(uris: List<Uri>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            var count = 0
            for (uri in uris) {
                val result = vaultRepository.importUriToVault(uri)
                if (result.isSuccess) count++
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                message = "Encrypted $count files into vault"
            )
        }
    }

    fun restoreItem(item: VaultEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = vaultRepository.restoreFromVault(item)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                message = if (result.isSuccess) "Restored ${item.originalFileName}" else "Failed to restore"
            )
        }
    }

    fun previewItem(item: VaultEntity, onReady: (File) -> Unit) {
        viewModelScope.launch {
            val result = vaultRepository.decryptForPreview(item)
            if (result.isSuccess) {
                val file = result.getOrNull()
                if (file != null) {
                    onReady(file)
                }
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
