package com.rentcollection.ui.screens.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentcollection.data.model.Payment
import com.rentcollection.data.repository.AuthRepository
import com.rentcollection.data.repository.BuildingRepository
import com.rentcollection.data.repository.PaymentRepository
import com.rentcollection.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class RecordPaymentState(
    val amount: String = "",
    val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val paidDate: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
    val paymentMode: String = "Cash",
    val notes: String = "",
    val isReceived: Boolean = true,
    val houseNumber: String = "",
    val buildingName: String = "",
    val tenantName: String = "",
    val floorName: String = "",
    val rentAmount: Double = 0.0,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null
)

val PAYMENT_MODES = listOf("Cash", "UPI", "Bank Transfer", "Cheque")

@HiltViewModel
class RecordPaymentViewModel @Inject constructor(
    private val buildingRepository: BuildingRepository,
    private val tenantRepository: TenantRepository,
    private val paymentRepository: PaymentRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val houseId: String = savedStateHandle["houseId"] ?: ""
    val buildingId: String = savedStateHandle["buildingId"] ?: ""
    val floorId: String = savedStateHandle["floorId"] ?: ""

    private val _state = MutableStateFlow(RecordPaymentState())
    val state: StateFlow<RecordPaymentState> = _state

    init {
        loadContext()
    }

    private fun loadContext() {
        viewModelScope.launch {
            val house = buildingRepository.getHouse(buildingId, floorId, houseId)
            val building = buildingRepository.getBuilding(buildingId)
            val floor = buildingRepository.getFloor(buildingId, floorId)
            val tenant = tenantRepository.getActiveTenantForHouseOnce(houseId)

            val floorDisplayName = floor?.let {
                it.floorName.ifEmpty { if (it.floorNumber > 0) "Floor ${it.floorNumber}" else "" }
            } ?: ""

            _state.update { s ->
                s.copy(
                    amount = house?.rentAmount?.let { if (it > 0) it.toString() else "" } ?: "",
                    rentAmount = house?.rentAmount ?: 0.0,
                    houseNumber = house?.houseNumber ?: "",
                    buildingName = building?.name ?: "",
                    tenantName = tenant?.name ?: "",
                    floorName = floorDisplayName,
                    isLoading = false
                )
            }
        }
    }

    fun onAmountChange(v: String) = _state.update { it.copy(amount = v) }
    fun onMonthChange(m: Int, y: Int) = _state.update { it.copy(month = m, year = y) }
    fun onPaidDateChange(v: String) = _state.update { it.copy(paidDate = v) }
    fun onPaymentModeChange(v: String) = _state.update { it.copy(paymentMode = v) }
    fun onNotesChange(v: String) = _state.update { it.copy(notes = v) }
    fun onIsReceivedChange(v: Boolean) = _state.update { it.copy(isReceived = v) }

    fun save() {
        val s = _state.value
        val amt = s.amount.toDoubleOrNull()
        if (amt == null || amt <= 0) {
            _state.update { it.copy(error = "Enter a valid amount") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val user = authRepository.currentUser
                val userProfile = authRepository.getCurrentUserProfile()
                val payment = Payment(
                    houseId = houseId,
                    buildingId = buildingId,
                    tenantId = "",
                    amount = amt,
                    month = s.month,
                    year = s.year,
                    paidDate = s.paidDate,
                    paymentMode = s.paymentMode,
                    notes = s.notes,
                    isReceived = s.isReceived,
                    recordedBy = userProfile?.name ?: user?.email ?: "Unknown",
                    recordedByEmail = user?.email ?: "",
                    houseNumber = s.houseNumber,
                    buildingName = s.buildingName,
                    tenantName = s.tenantName,
                    floorId = floorId,
                    floorName = s.floorName
                )
                paymentRepository.recordPayment(payment)
                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
