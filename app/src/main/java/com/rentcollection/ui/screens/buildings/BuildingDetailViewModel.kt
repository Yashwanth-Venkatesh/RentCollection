package com.rentcollection.ui.screens.buildings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentcollection.data.model.Building
import com.rentcollection.data.model.Floor
import com.rentcollection.data.model.House
import com.rentcollection.data.model.Payment
import com.rentcollection.data.repository.BuildingRepository
import com.rentcollection.data.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HouseWithPayment(
    val house: House,
    val payment: Payment?,
    val isPaid: Boolean = payment?.isReceived == true
)

data class FloorWithHouses(
    val floor: Floor,
    val houses: List<HouseWithPayment>,
    val isExpanded: Boolean = true
)

data class BuildingDetailState(
    val building: Building? = null,
    val floors: List<FloorWithHouses> = emptyList(),
    val totalCollected: Double = 0.0,
    val totalPending: Double = 0.0,
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class BuildingDetailViewModel @Inject constructor(
    private val buildingRepository: BuildingRepository,
    private val paymentRepository: PaymentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val buildingId: String = savedStateHandle["buildingId"] ?: ""

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    // Tracks collapsed floors; floors not in this set are expanded (default: all expanded)
    private val _collapsedFloors = MutableStateFlow<Set<String>>(emptySet())

    private val _state = MutableStateFlow(BuildingDetailState())
    val state: StateFlow<BuildingDetailState> = _state

    init {
        loadBuilding()
        observeData()
    }

    private fun loadBuilding() {
        viewModelScope.launch {
            val building = buildingRepository.getBuilding(buildingId)
            _state.update { it.copy(building = building) }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(_selectedMonth, _selectedYear) { m, y -> m to y }
                .collectLatest { (month, year) ->
                    combine(
                        buildingRepository.getFloorsFlow(buildingId),
                        paymentRepository.getPaymentsForBuildingMonthFlow(buildingId, month, year)
                    ) { floors, payments -> floors to payments }
                    .collectLatest { (floors, payments) ->
                        val collected = payments.filter { it.isReceived }.sumOf { it.amount }
                        val pending = payments.filter { !it.isReceived }.sumOf { it.amount }

                        if (floors.isEmpty()) {
                            _state.update { it.copy(
                                floors = emptyList(),
                                totalCollected = collected,
                                totalPending = pending,
                                selectedMonth = month,
                                selectedYear = year,
                                isLoading = false
                            )}
                            return@collectLatest
                        }

                        // Listen to houses for every floor simultaneously
                        combine(
                            floors.map { floor ->
                                buildingRepository.getHousesFlow(buildingId, floor.floorId)
                                    .map { houses -> floor to houses }
                            }
                        ) { it.toList() }
                        .collect { floorHousePairs ->
                            val collapsed = _collapsedFloors.value
                            val floorsWithHouses = floorHousePairs.map { (floor, houses) ->
                                FloorWithHouses(
                                    floor = floor,
                                    houses = houses.map { house ->
                                        val payment = payments.find { it.houseId == house.houseId }
                                        HouseWithPayment(house, payment, payment?.isReceived == true)
                                    },
                                    isExpanded = floor.floorId !in collapsed
                                )
                            }
                            _state.update { prev ->
                                prev.copy(
                                    floors = floorsWithHouses,
                                    totalCollected = collected,
                                    totalPending = pending,
                                    selectedMonth = month,
                                    selectedYear = year,
                                    isLoading = false
                                )
                            }
                        }
                    }
                }
        }
    }

    fun toggleFloor(floorId: String) {
        _collapsedFloors.update { current ->
            if (floorId in current) current - floorId else current + floorId
        }
        _state.update { prev ->
            prev.copy(floors = prev.floors.map { f ->
                if (f.floor.floorId == floorId) f.copy(isExpanded = !f.isExpanded) else f
            })
        }
    }

    fun setMonth(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    fun bulkMarkFloorPaid(floorId: String) {
        viewModelScope.launch {
            try {
                val today = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                val floor = _state.value.floors.find { it.floor.floorId == floorId } ?: return@launch
                val month = _state.value.selectedMonth
                val year = _state.value.selectedYear
                floor.houses.forEach { hwp ->
                    if (!hwp.isPaid) {
                        if (hwp.payment != null && hwp.payment.paymentId.isNotEmpty()) {
                            paymentRepository.markAsReceived(hwp.payment.paymentId, today)
                        } else {
                            paymentRepository.recordPayment(
                                Payment(
                                    houseId = hwp.house.houseId,
                                    buildingId = buildingId,
                                    floorId = floorId,
                                    houseNumber = hwp.house.houseNumber,
                                    amount = hwp.house.rentAmount,
                                    month = month,
                                    year = year,
                                    isReceived = true,
                                    paidDate = today
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "Failed to update payment") }
            }
        }
    }

    fun clearError() = _state.update { it.copy(errorMessage = null) }
}
