package com.rentcollection.ui.screens.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentcollection.data.model.House
import com.rentcollection.data.model.RentChange
import com.rentcollection.data.repository.BuildingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditHouseState(
    val houseNumber: String = "",
    val rentAmount: String = "",
    val depositAmount: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isEditing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddEditHouseViewModel @Inject constructor(
    private val buildingRepository: BuildingRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val buildingId: String = savedStateHandle["buildingId"] ?: ""
    val floorId: String = savedStateHandle["floorId"] ?: ""
    private val houseId: String? = savedStateHandle["houseId"]

    private val _state = MutableStateFlow(AddEditHouseState())
    val state: StateFlow<AddEditHouseState> = _state

    init {
        houseId?.let { loadHouse(it) }
    }

    private fun loadHouse(id: String) {
        viewModelScope.launch {
            val h = buildingRepository.getHouse(buildingId, floorId, id)
            h?.let {
                _state.update { s ->
                    s.copy(
                        houseNumber = h.houseNumber,
                        rentAmount = h.rentAmount.let { if (it == 0.0) "" else it.toString() },
                        depositAmount = h.depositAmount.let { if (it == 0.0) "" else it.toString() },
                        isEditing = true
                    )
                }
            }
        }
    }

    fun onHouseNumberChange(v: String) = _state.update { it.copy(houseNumber = v) }
    fun onRentAmountChange(v: String) = _state.update { it.copy(rentAmount = v) }
    fun onDepositAmountChange(v: String) = _state.update { it.copy(depositAmount = v) }

    fun save() {
        val s = _state.value
        if (s.houseNumber.isBlank()) {
            _state.update { it.copy(error = "House number is required") }
            return
        }
        val newRent = s.rentAmount.toDoubleOrNull() ?: 0.0
        val deposit = s.depositAmount.toDoubleOrNull() ?: 0.0
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                if (houseId != null) {
                    val existing = buildingRepository.getHouse(buildingId, floorId, houseId) ?: House()
                    val cal = java.util.Calendar.getInstance()
                    val curYear = cal.get(java.util.Calendar.YEAR)
                    // When rent changes, record the old rent with its effective period ending now
                    val newHistory = if (existing.rentAmount > 0 && existing.rentAmount != newRent) {
                        val oldEntry = if (existing.rentChanges.isEmpty()) {
                            RentChange(existing.rentAmount, 1, curYear)
                        } else {
                            RentChange(
                                existing.rentAmount,
                                existing.rentChanges.last().effectiveFromMonth,
                                existing.rentChanges.last().effectiveFromYear
                            )
                        }
                        existing.rentChanges + oldEntry
                    } else {
                        existing.rentChanges
                    }
                    buildingRepository.updateHouse(
                        buildingId, floorId,
                        existing.copy(
                            houseNumber = s.houseNumber,
                            rentAmount = newRent,
                            depositAmount = deposit,
                            rentChanges = newHistory
                        )
                    )
                } else {
                    buildingRepository.addHouse(
                        buildingId, floorId,
                        House(houseNumber = s.houseNumber, rentAmount = newRent, depositAmount = deposit)
                    )
                }
                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
