package com.rentcollection.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentcollection.data.repository.BuildingRepository
import com.rentcollection.data.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val buildingRepository: BuildingRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    init {
        observePendingCount()
    }

    private fun observePendingCount() {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)
        viewModelScope.launch {
            combine(
                buildingRepository.getAllHousesFlow(),
                paymentRepository.getPaymentsForMonthFlow(month, year)
            ) { houses, payments ->
                houses.count { house ->
                    val p = payments.find { it.houseId == house.houseId }
                    p == null || !p.isReceived
                }
            }.collect { _pendingCount.value = it }
        }
    }
}
