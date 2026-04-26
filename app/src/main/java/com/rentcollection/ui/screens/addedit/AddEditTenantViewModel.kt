package com.rentcollection.ui.screens.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentcollection.data.model.Tenant
import com.rentcollection.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditTenantState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val alternatePhone: String = "",
    val moveInDate: String = "",
    val moveOutDate: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isEditing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddEditTenantViewModel @Inject constructor(
    private val tenantRepository: TenantRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val houseId: String = savedStateHandle["houseId"] ?: ""
    val buildingId: String = savedStateHandle["buildingId"] ?: ""
    private val tenantId: String? = savedStateHandle["tenantId"]

    private val _state = MutableStateFlow(AddEditTenantState())
    val state: StateFlow<AddEditTenantState> = _state

    init {
        tenantId?.let { loadTenant(it) }
            ?: run {
                // Try loading the current active tenant even if no tenantId passed
                viewModelScope.launch {
                    val t = tenantRepository.getActiveTenantForHouseOnce(houseId)
                    t?.let { loadFromTenant(it) }
                }
            }
    }

    private fun loadTenant(id: String) {
        viewModelScope.launch {
            val t = tenantRepository.getTenant(id)
            t?.let { loadFromTenant(it) }
        }
    }

    private fun loadFromTenant(t: Tenant) {
        _state.update { s ->
            s.copy(
                name = t.name, phone = t.phone, email = t.email,
                alternatePhone = t.alternatePhone, moveInDate = t.moveInDate,
                moveOutDate = t.moveOutDate, notes = t.notes,
                isActive = t.isActive, isEditing = true
            )
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onPhoneChange(v: String) = _state.update { it.copy(phone = v) }
    fun onEmailChange(v: String) = _state.update { it.copy(email = v) }
    fun onAltPhoneChange(v: String) = _state.update { it.copy(alternatePhone = v) }
    fun onMoveInChange(v: String) = _state.update { it.copy(moveInDate = v) }
    fun onMoveOutChange(v: String) = _state.update { it.copy(moveOutDate = v) }
    fun onNotesChange(v: String) = _state.update { it.copy(notes = v) }
    fun onIsActiveChange(v: Boolean) = _state.update { it.copy(isActive = v) }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.update { it.copy(error = "Tenant name is required") }; return }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val existingTenant = tenantId?.let { tenantRepository.getTenant(it) }
                    ?: tenantRepository.getActiveTenantForHouseOnce(houseId)
                val tenant = Tenant(
                    tenantId = existingTenant?.tenantId ?: "",
                    houseId = houseId, buildingId = buildingId,
                    name = s.name, phone = s.phone, email = s.email,
                    alternatePhone = s.alternatePhone, moveInDate = s.moveInDate,
                    moveOutDate = s.moveOutDate, notes = s.notes, isActive = s.isActive
                )
                if (existingTenant != null) {
                    tenantRepository.updateTenant(tenant)
                } else {
                    tenantRepository.addTenant(tenant)
                }
                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
