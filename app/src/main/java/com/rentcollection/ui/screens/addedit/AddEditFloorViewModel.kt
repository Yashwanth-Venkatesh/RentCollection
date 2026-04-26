package com.rentcollection.ui.screens.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentcollection.data.model.Floor
import com.rentcollection.data.repository.BuildingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditFloorState(
    val floorNumber: String = "",
    val floorName: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isEditing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddEditFloorViewModel @Inject constructor(
    private val buildingRepository: BuildingRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val buildingId: String = savedStateHandle["buildingId"] ?: ""
    private val floorId: String? = savedStateHandle["floorId"]

    private val _state = MutableStateFlow(AddEditFloorState())
    val state: StateFlow<AddEditFloorState> = _state

    init {
        floorId?.let { loadFloor(it) }
    }

    private fun loadFloor(id: String) {
        viewModelScope.launch {
            val f = buildingRepository.getFloor(buildingId, id)
            f?.let {
                _state.update { s ->
                    s.copy(
                        floorNumber = f.floorNumber.toString(),
                        floorName = f.floorName,
                        isEditing = true
                    )
                }
            }
        }
    }

    fun onFloorNumberChange(v: String) = _state.update { it.copy(floorNumber = v) }
    fun onFloorNameChange(v: String) = _state.update { it.copy(floorName = v) }

    fun save() {
        val s = _state.value
        val num = s.floorNumber.toIntOrNull()
        if (num == null) { _state.update { it.copy(error = "Enter a valid floor number") }; return }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                if (floorId != null) {
                    val existing = buildingRepository.getFloor(buildingId, floorId) ?: Floor()
                    buildingRepository.updateFloor(
                        buildingId,
                        existing.copy(floorNumber = num, floorName = s.floorName)
                    )
                } else {
                    buildingRepository.addFloor(
                        buildingId,
                        Floor(floorNumber = num, floorName = s.floorName.ifBlank { "Floor $num" })
                    )
                }
                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
