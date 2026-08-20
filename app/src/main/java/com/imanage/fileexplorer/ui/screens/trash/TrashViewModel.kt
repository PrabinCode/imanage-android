package com.imanage.fileexplorer.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imanage.fileexplorer.data.local.entity.TrashEntity
import com.imanage.fileexplorer.data.repository.TrashRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrashUiState(
    val items: List<TrashEntity> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null
)

class TrashViewModel(
    private val trashRepository: TrashRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        observeTrash()
    }

    private fun observeTrash() {
        viewModelScope.launch {
            trashRepository.trashItems.collect { list ->
                _uiState.value = _uiState.value.copy(items = list)
            }
        }
    }

    fun restore(item: TrashEntity) {
        viewModelScope.launch {
            val result = trashRepository.restoreItem(item)
            _uiState.value = _uiState.value.copy(
                message = if (result.isSuccess) "Restored ${item.originalName}" else "Failed to restore"
            )
        }
    }

    fun permanentlyDelete(item: TrashEntity, shred: Boolean = true) {
        viewModelScope.launch {
            trashRepository.permanentlyDelete(item, shred)
        }
    }

    fun emptyTrash(shred: Boolean = true) {
        viewModelScope.launch {
            trashRepository.emptyTrash(shred)
        }
    }
}
