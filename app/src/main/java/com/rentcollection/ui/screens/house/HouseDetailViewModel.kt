package com.rentcollection.ui.screens.house

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentcollection.data.model.House
import com.rentcollection.data.model.Payment
import com.rentcollection.data.model.Tenant
import com.rentcollection.data.repository.BuildingRepository
import com.rentcollection.data.repository.PaymentRepository
import com.rentcollection.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HouseDetailState(
    val house: House? = null,
    val tenant: Tenant? = null,
    val currentMonthPayment: Payment? = null,
    val paymentHistory: List<Payment> = emptyList(),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val isLoading: Boolean = true,
    val buildingName: String = ""
)

@HiltViewModel
class HouseDetailViewModel @Inject constructor(
    private val buildingRepository: BuildingRepository,
    private val tenantRepository: TenantRepository,
    private val paymentRepository: PaymentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val buildingId: String = savedStateHandle["buildingId"] ?: ""
    val floorId: String = savedStateHandle["floorId"] ?: ""
    val houseId: String = savedStateHandle["houseId"] ?: ""

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))

    private val _state = MutableStateFlow(HouseDetailState())
    val state: StateFlow<HouseDetailState> = _state

    init {
        loadStaticData()
        observeTenant()
        observePaymentHistory()
        observeCurrentMonthPayment()
    }

    private fun loadStaticData() {
        viewModelScope.launch {
            val house = buildingRepository.getHouse(buildingId, floorId, houseId)
            val building = buildingRepository.getBuilding(buildingId)
            _state.update {
                it.copy(
                    house = house,
                    buildingName = building?.name ?: "",
                    isLoading = false
                )
            }
        }
    }

    private fun observeTenant() {
        viewModelScope.launch {
            tenantRepository.getActiveTenantForHouseFlow(houseId)
                .catch { emit(null) }
                .collect { tenant ->
                    _state.update { it.copy(tenant = tenant) }
                }
        }
    }

    private fun observePaymentHistory() {
        viewModelScope.launch {
            paymentRepository.getPaymentsForHouseFlow(houseId)
                .collect { history ->
                    _state.update { it.copy(paymentHistory = history) }
                }
        }
    }

    private fun observeCurrentMonthPayment() {
        viewModelScope.launch {
            combine(_selectedMonth, _selectedYear) { m, y -> m to y }
                .collectLatest { (month, year) ->
                    paymentRepository.getPaymentForHouseMonthFlow(houseId, month, year)
                        .collect { payment ->
                            _state.update { it.copy(currentMonthPayment = payment, selectedMonth = month, selectedYear = year) }
                        }
                }
        }
    }

    fun setMonth(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }
}
