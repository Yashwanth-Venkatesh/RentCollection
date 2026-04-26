package com.rentcollection.ui.screens.buildings

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

data class BuildingWithStats(
    val building: Building,
    val collected: Double = 0.0,
    val pending: Double = 0.0,
    val paymentCount: Int = 0
)

data class BuildingsState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val buildings: List<BuildingWithStats> = emptyList(),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val searchQuery: String = ""
)

@HiltViewModel
class BuildingsViewModel @Inject constructor(
    private val buildingRepository: BuildingRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _refreshTrigger = MutableStateFlow(0)
    private val _searchQuery = MutableStateFlow("")

    private val _state = MutableStateFlow(BuildingsState())
    val state: StateFlow<BuildingsState> = _state

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(_selectedMonth, _selectedYear, _refreshTrigger, _searchQuery) { m, y, _, q ->
                Triple(m, y, q)
            }.collectLatest { (month, year, query) ->
                combine(
                    buildingRepository.getBuildingsFlow(),
                    paymentRepository.getPaymentsForMonthFlow(month, year)
                ) { buildings, payments ->
                    val filtered = if (query.isBlank()) buildings
                    else buildings.filter { it.name.contains(query, ignoreCase = true) }
                    filtered.map { b ->
                        val bPayments = payments.filter { it.buildingId == b.buildingId }
                        BuildingWithStats(
                            building = b,
                            collected = bPayments.filter { it.isReceived }.sumOf { it.amount },
                            pending = bPayments.filter { !it.isReceived }.sumOf { it.amount },
                            paymentCount = bPayments.size
                        )
                    }
                }.collect { stats ->
                    _state.value = BuildingsState(
                        isLoading = false,
                        buildings = stats,
                        selectedMonth = month,
                        selectedYear = year,
                        searchQuery = query
                    )
                }
            }
        }
    }

    fun setMonth(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    fun setSearch(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        _refreshTrigger.value++
        viewModelScope.launch {
            delay(900)
            _state.update { it.copy(isRefreshing = false) }
        }
    }
}
