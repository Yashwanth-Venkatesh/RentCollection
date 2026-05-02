package com.rentcollection.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentcollection.data.model.Building
import com.rentcollection.data.model.House
import com.rentcollection.data.model.Payment
import com.rentcollection.data.repository.AuthRepository
import com.rentcollection.data.repository.BuildingRepository
import com.rentcollection.data.repository.PaymentRepository
import com.rentcollection.data.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MonthlyTrend(
    val month: Int,
    val year: Int,
    val label: String,
    val collected: Double,
    val total: Double
)

data class DashboardState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val totalCollected: Double = 0.0,
    val totalPending: Double = 0.0,
    val pendingCount: Int = 0,
    val totalBuildings: Int = 0,
    val collectionPercentage: Float = 0f,
    val pendingPayments: List<Payment> = emptyList(),
    val buildings: List<Building> = emptyList(),
    val selectedBuildingId: String? = null,
    val userName: String = "",
    val userRole: String = "",
    val undoPayment: Payment? = null,
    val trendData: List<MonthlyTrend> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val buildingRepository: BuildingRepository,
    private val paymentRepository: PaymentRepository,
    private val tenantRepository: TenantRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedBuildingId = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state

    init {
        loadUserProfile()
        observeData()
        observeTrend()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val profile = authRepository.getCurrentUserProfile()
            _state.update { it.copy(userName = profile?.name ?: "", userRole = profile?.role ?: "") }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(_selectedMonth, _selectedYear, _selectedBuildingId) { m, y, bid ->
                Triple(m, y, bid)
            }.collectLatest { (month, year, buildingId) ->
                combine(
                    buildingRepository.getBuildingsFlow(),
                    buildingRepository.getAllFloorsFlow(),
                    buildingRepository.getAllHousesFlow(),
                    paymentRepository.getPaymentsForMonthFlow(month, year),
                    tenantRepository.getAllActiveTenantsFlow()
                ) { buildings, allFloors, allHouses, allPayments, activeTenants ->

                    val filteredHouses = if (buildingId != null)
                        allHouses.filter { it.buildingId == buildingId }
                    else allHouses

                    val filteredPayments = if (buildingId != null)
                        allPayments.filter { it.buildingId == buildingId }
                    else allPayments

                    val collected = filteredPayments.filter { it.isReceived }.sumOf { it.amount }

                    val pending = filteredHouses.mapNotNull { house ->
                        val payment = filteredPayments.find { it.houseId == house.houseId }
                        val building = buildings.find { it.buildingId == house.buildingId }
                        val floor = allFloors.find { it.floorId == house.floorId }
                        val floorDisplay = floor?.let {
                            it.floorName.ifEmpty {
                                if (it.floorNumber > 0) "Floor ${it.floorNumber}" else ""
                            }
                        } ?: ""
                        val tenant = activeTenants.firstOrNull { it.houseId == house.houseId }
                        val rentForMonth = house.rentForMonth(month, year)

                        when {
                            payment == null -> Payment(
                                houseId = house.houseId,
                                buildingId = house.buildingId,
                                houseNumber = house.houseNumber,
                                buildingName = building?.name ?: "",
                                floorId = house.floorId,
                                floorName = floorDisplay,
                                tenantName = tenant?.name ?: "",
                                tenantPhone = tenant?.phone ?: "",
                                amount = rentForMonth,
                                month = month,
                                year = year
                            )
                            !payment.isReceived -> {
                                var enriched = if (payment.tenantName.isEmpty() && tenant != null)
                                    payment.copy(tenantName = tenant.name) else payment
                                enriched = if (enriched.tenantPhone.isEmpty() && tenant != null)
                                    enriched.copy(tenantPhone = tenant.phone) else enriched
                                val withFloor = if (enriched.floorName.isEmpty() && floorDisplay.isNotEmpty())
                                    enriched.copy(floorName = floorDisplay) else enriched
                                withFloor
                            }
                            else -> null
                        }
                    }

                    val pendingAmt = pending.sumOf { it.amount }
                    val total = collected + pendingAmt

                    _state.value.copy(
                        isLoading = false,
                        selectedMonth = month,
                        selectedYear = year,
                        totalCollected = collected,
                        totalPending = pendingAmt,
                        pendingCount = pending.size,
                        totalBuildings = buildings.size,
                        collectionPercentage = if (total > 0) (collected / total).toFloat() else 0f,
                        pendingPayments = pending.sortedWith(
                            compareBy({ it.buildingName }, { it.houseNumber })
                        ),
                        buildings = buildings.sortedBy { it.name },
                        selectedBuildingId = buildingId
                    )
                }.collect { newState ->
                    _state.value = newState
                }
            }
        }
    }

    private fun observeTrend() {
        val cal = Calendar.getInstance()
        val months = (0 until 6).map {
            val m = cal.get(Calendar.MONTH) + 1
            val y = cal.get(Calendar.YEAR)
            cal.add(Calendar.MONTH, -1)
            m to y
        }.reversed()
        val years = months.map { it.second }.distinct()
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        viewModelScope.launch {
            paymentRepository.getPaymentsForYearsFlow(years).collect { payments ->
                val trend = months.map { (month, year) ->
                    val mp = payments.filter { it.month == month && it.year == year }
                    val col = mp.filter { it.isReceived }.sumOf { it.amount }
                    val tot = mp.sumOf { it.amount }
                    MonthlyTrend(month, year, monthNames[month - 1], col, tot)
                }
                _state.update { it.copy(trendData = trend) }
            }
        }
    }

    fun setMonth(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
    }

    fun setBuilding(buildingId: String?) {
        _selectedBuildingId.value = buildingId
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            delay(900)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun markAsPaid(payment: Payment) {
        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                if (payment.paymentId.isNotEmpty()) {
                    paymentRepository.markAsReceived(payment.paymentId, today)
                    _state.update { it.copy(undoPayment = payment) }
                } else {
                    val newId = paymentRepository.recordPayment(
                        payment.copy(
                            isReceived = true,
                            paidDate = today,
                            recordedBy = _state.value.userName
                        )
                    )
                    _state.update { it.copy(undoPayment = payment.copy(paymentId = newId)) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.message ?: "Failed to update payment") }
            }
        }
    }

    fun undoMarkAsPaid() {
        val p = _state.value.undoPayment ?: return
        _state.update { it.copy(undoPayment = null) }
        viewModelScope.launch {
            try {
                paymentRepository.undoMarkAsReceived(p.paymentId)
            } catch (_: Exception) {}
        }
    }

    fun clearUndo() = _state.update { it.copy(undoPayment = null) }
    fun clearError() = _state.update { it.copy(errorMessage = null) }

    fun logout() = authRepository.logout()
}

private fun House.rentForMonth(month: Int, year: Int): Double {
    if (rentChanges.isEmpty()) return rentAmount
    val requestedIdx = year * 12 + month
    // Sort changes by effective date
    val sorted = rentChanges.sortedWith(compareBy { it.effectiveFromYear * 12 + it.effectiveFromMonth })
    // The current rentAmount started AFTER the last entry
    val lastChangeIdx = sorted.lastOrNull()?.let { it.effectiveFromYear * 12 + it.effectiveFromMonth } ?: 0
    if (requestedIdx >= lastChangeIdx) return rentAmount
    // Find the applicable historical rent
    val applicable = sorted.lastOrNull { it.effectiveFromYear * 12 + it.effectiveFromMonth <= requestedIdx }
    return applicable?.amount ?: rentAmount
}
