package com.rentcollection.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentcollection.data.model.Building
import com.rentcollection.data.model.Payment
import com.rentcollection.data.repository.BuildingRepository
import com.rentcollection.data.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class PaymentHistoryState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val allPayments: List<Payment> = emptyList(),
    val filteredPayments: List<Payment> = emptyList(),
    val buildings: List<Building> = emptyList(),
    val selectedBuildingId: String? = null,
    val selectedMonth: Int? = null,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
)

@HiltViewModel
class PaymentHistoryViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val buildingRepository: BuildingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentHistoryState())
    val state: StateFlow<PaymentHistoryState> = _state

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                paymentRepository.getAllPaymentsFlow(),
                buildingRepository.getBuildingsFlow()
            ) { payments, buildings ->
                val s = _state.value
                s.copy(
                    allPayments = payments,
                    filteredPayments = applyFilters(payments, s.selectedBuildingId, s.selectedMonth, s.selectedYear),
                    buildings = buildings,
                    isLoading = false
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    private fun applyFilters(
        payments: List<Payment>,
        buildingId: String?,
        month: Int?,
        year: Int
    ): List<Payment> = payments.filter { p ->
        (buildingId == null || p.buildingId == buildingId) &&
        (month == null || (p.month == month && p.year == year))
    }

    fun setBuilding(buildingId: String?) {
        _state.update { s ->
            s.copy(
                selectedBuildingId = buildingId,
                filteredPayments = applyFilters(s.allPayments, buildingId, s.selectedMonth, s.selectedYear)
            )
        }
    }

    fun setMonthYear(month: Int?, year: Int) {
        _state.update { s ->
            s.copy(
                selectedMonth = month,
                selectedYear = year,
                filteredPayments = applyFilters(s.allPayments, s.selectedBuildingId, month, year)
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            delay(900)
            _state.update { it.copy(isRefreshing = false) }
        }
    }
}
