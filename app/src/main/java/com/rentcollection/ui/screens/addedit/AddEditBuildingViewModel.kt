package com.rentcollection.ui.screens.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentcollection.data.model.Building
import com.rentcollection.data.repository.AuthRepository
import com.rentcollection.data.repository.BuildingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditBuildingState(
    val name: String = "",
    val address: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false
)

@HiltViewModel
class AddEditBuildingViewModel @Inject constructor(
    private val buildingRepository: BuildingRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val buildingId: String? = savedStateHandle["buildingId"]

    private val _state = MutableStateFlow(AddEditBuildingState())
    val state: StateFlow<AddEditBuildingState> = _state

    init {
        buildingId?.let { loadBuilding(it) }
    }

    private fun loadBuilding(id: String) {
        viewModelScope.launch {
            val b = buildingRepository.getBuilding(id)
            b?.let {
                _state.update { s ->
                    s.copy(name = b.name, address = b.address, description = b.description, isEditing = true)
                }
            }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onAddressChange(v: String) = _state.update { it.copy(address = v) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.update { it.copy(error = "Building name is required") }; return }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val uid = authRepository.currentUser?.uid ?: ""
                if (buildingId != null) {
                    val existing = buildingRepository.getBuilding(buildingId) ?: Building()
                    buildingRepository.updateBuilding(
                        existing.copy(name = s.name, address = s.address, description = s.description)
                    )
                } else {
                    buildingRepository.addBuilding(
                        Building(name = s.name, address = s.address, description = s.description, createdBy = uid)
                    )
                }
                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
