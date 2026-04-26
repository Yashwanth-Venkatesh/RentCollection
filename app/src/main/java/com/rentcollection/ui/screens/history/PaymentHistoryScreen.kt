@file:OptIn(ExperimentalMaterial3Api::class)

package com.rentcollection.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rentcollection.data.model.Payment
import com.rentcollection.ui.components.EmptyState
import com.rentcollection.ui.components.LoadingScreen
import com.rentcollection.ui.screens.dashboard.MonthYearPickerDialog
import com.rentcollection.ui.theme.AppColors
import com.rentcollection.ui.theme.LocalDarkTheme
import com.rentcollection.utils.toFullMonthName
import com.rentcollection.utils.toMonthName
import com.rentcollection.utils.toRupees

@Composable
fun PaymentHistoryScreen(
    navController: NavController,
    viewModel: PaymentHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showMonthPicker by remember { mutableStateOf(false) }
    var buildingMenuExpanded by remember { mutableStateOf(false) }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            currentMonth = state.selectedMonth ?: 1,
            currentYear = state.selectedYear,
            onDismiss = { showMonthPicker = false },
            onConfirm = { m, y -> viewModel.setMonthYear(m, y); showMonthPicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment History", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (state.isLoading) { LoadingScreen(Modifier.padding(padding)); return@Scaffold }

        val pullRefreshState = rememberPullToRefreshState()
        LaunchedEffect(pullRefreshState.isRefreshing) {
            if (pullRefreshState.isRefreshing) viewModel.refresh()
        }
        LaunchedEffect(state.isRefreshing) {
            if (!state.isRefreshing) pullRefreshState.endRefresh()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showMonthPicker = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (state.selectedMonth != null)
                                "${state.selectedMonth!!.toMonthName()} ${state.selectedYear}"
                            else "All History",
                            fontSize = 13.sp
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    if (state.selectedMonth != null) {
                        OutlinedButton(
                            onClick = { viewModel.setMonthYear(null, state.selectedYear) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("All", fontSize = 13.sp)
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { buildingMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                state.buildings.find { it.buildingId == state.selectedBuildingId }?.name
                                    ?: "All Buildings",
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = buildingMenuExpanded,
                            onDismissRequest = { buildingMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Buildings") },
                                onClick = { viewModel.setBuilding(null); buildingMenuExpanded = false }
                            )
                            state.buildings.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b.name) },
                                    onClick = { viewModel.setBuilding(b.buildingId); buildingMenuExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Summary bar
                val totalCollected = state.filteredPayments.filter { it.isReceived }.sumOf { it.amount }
                val totalPending = state.filteredPayments.filter { !it.isReceived }.sumOf { it.amount }
                val isDarkSummary = LocalDarkTheme.current
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${state.filteredPayments.size} entries", fontSize = 12.sp)
                        Text(
                            "Collected: ${totalCollected.toRupees()}",
                            fontSize = 12.sp,
                            color = if (isDarkSummary) AppColors.CollectedBadge else AppColors.Collected
                        )
                        Text(
                            "Pending: ${totalPending.toRupees()}",
                            fontSize = 12.sp,
                            color = if (isDarkSummary) AppColors.PendingBadge else AppColors.Pending
                        )
                    }
                }

                if (state.filteredPayments.isEmpty()) {
                    EmptyState(
                        message = "No payments found\nfor the selected filters.",
                        icon = Icons.Default.Receipt
                    )
                } else {
                    val showGroupHeaders = state.selectedMonth == null
                    val grouped: List<Pair<Pair<Int, Int>?, List<Payment>>> = if (showGroupHeaders) {
                        state.filteredPayments
                            .groupBy { it.year to it.month }
                            .entries
                            .sortedWith(compareByDescending<Map.Entry<Pair<Int, Int>, List<Payment>>> { it.key.first }
                                .thenByDescending { it.key.second })
                            .map { it.key to it.value }
                    } else {
                        listOf(null to state.filteredPayments)
                    }

                    LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        grouped.forEach { (key, payments) ->
                            if (key != null) {
                                item(key = "header_${key.first}_${key.second}") {
                                    Text(
                                        "${key.second.toFullMonthName()} ${key.first}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                                    )
                                }
                            }
                            items(payments, key = { it.paymentId.ifEmpty { "p_${it.houseId}_${it.month}_${it.year}" } }) { p ->
                                PaymentCard(p)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun PaymentCard(payment: Payment) {
    val isPaid = payment.isReceived
    val isDark = LocalDarkTheme.current
    val cardBg = when {
        isPaid && isDark -> AppColors.CollectedDark
        isPaid           -> AppColors.CollectedLight
        isDark           -> AppColors.WarningDark
        else             -> AppColors.WarningLight
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardBg,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isPaid) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
                contentDescription = null,
                tint = if (isPaid) AppColors.CollectedBadge else AppColors.Warning,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        payment.houseNumber.ifEmpty { "House" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    if (payment.buildingName.isNotEmpty()) {
                        Text(
                            " · ${payment.buildingName}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Text(
                    "${payment.month.toMonthName()} ${payment.year}" +
                    if (payment.tenantName.isNotEmpty()) " · ${payment.tenantName}" else "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    payment.paymentMode +
                    if (payment.recordedBy.isNotEmpty()) " · by ${payment.recordedBy}" else "",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    payment.amount.toRupees(),
                    fontWeight = FontWeight.Bold,
                    color = if (isPaid) AppColors.CollectedBadge else AppColors.Warning,
                    fontSize = 14.sp
                )
                Text(
                    if (isPaid) "PAID" else "PENDING",
                    fontSize = 10.sp,
                    color = if (isPaid) AppColors.CollectedBadge else AppColors.Warning,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
