package com.rentcollection.ui.screens.buildings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rentcollection.ui.components.EmptyState
import com.rentcollection.ui.components.LoadingScreen
import com.rentcollection.ui.navigation.Screen
import com.rentcollection.ui.screens.dashboard.MonthYearPickerDialog
import com.rentcollection.ui.theme.AppColors
import com.rentcollection.utils.toFullMonthName
import com.rentcollection.utils.toRupees

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingDetailScreen(
    navController: NavController,
    buildingId: String,
    viewModel: BuildingDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showMonthPicker by remember { mutableStateOf(false) }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            currentMonth = state.selectedMonth,
            currentYear = state.selectedYear,
            onDismiss = { showMonthPicker = false },
            onConfirm = { m, y -> viewModel.setMonth(m, y); showMonthPicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.building?.name ?: "Building", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screen.AddEditFloor.createRoute(buildingId))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Floor")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (state.isLoading) { LoadingScreen(Modifier.padding(padding)); return@Scaffold }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Building info header
            state.building?.let { b ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (b.address.isNotEmpty()) {
                                Text(b.address, fontSize = 13.sp)
                            }
                            if (b.description.isNotEmpty()) {
                                Text(b.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }

            // Month filter
            item {
                OutlinedButton(
                    onClick = { showMonthPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${state.selectedMonth.toFullMonthName()} ${state.selectedYear}",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }

            // Collection summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryChip(Modifier.weight(1f), "Collected", state.totalCollected.toRupees(), AppColors.Collected)
                    SummaryChip(Modifier.weight(1f), "Pending", state.totalPending.toRupees(), AppColors.Pending)
                }
            }

            if (state.floors.isEmpty()) {
                item {
                    EmptyState(
                        message = "No floors yet.\nTap + to add a floor.",
                        icon = Icons.Default.Apartment
                    )
                }
            } else {
                items(state.floors) { floorWithHouses ->
                    FloorSection(
                        floorWithHouses = floorWithHouses,
                        buildingId = buildingId,
                        navController = navController,
                        onToggle = { viewModel.toggleFloor(floorWithHouses.floor.floorId) },
                        onAddHouse = {
                            navController.navigate(
                                Screen.AddEditHouse.createRoute(buildingId, floorWithHouses.floor.floorId)
                            )
                        },
                        onEditFloor = {
                            navController.navigate(
                                Screen.AddEditFloor.createRoute(buildingId, floorWithHouses.floor.floorId)
                            )
                        },
                        onBulkMarkPaid = { viewModel.bulkMarkFloorPaid(floorWithHouses.floor.floorId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(modifier: Modifier, label: String, value: String, color: Color) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
        }
    }
}

@Composable
private fun FloorSection(
    floorWithHouses: FloorWithHouses,
    buildingId: String,
    navController: NavController,
    onToggle: () -> Unit,
    onAddHouse: () -> Unit,
    onEditFloor: () -> Unit,
    onBulkMarkPaid: () -> Unit = {}
) {
    val floor = floorWithHouses.floor
    val paidCount = floorWithHouses.houses.count { it.isPaid }
    val totalCount = floorWithHouses.houses.size
    val progress = if (totalCount > 0) paidCount.toFloat() / totalCount else 0f
    val hasUnpaid = floorWithHouses.houses.any { !it.isPaid }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Floor header row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        floor.floorName.ifEmpty { "Floor ${floor.floorNumber}" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    if (totalCount > 0) {
                        Text(
                            "$paidCount / $totalCount paid",
                            fontSize = 11.sp,
                            color = if (paidCount == totalCount) AppColors.Collected
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                }
                if (hasUnpaid && totalCount > 0) {
                    TextButton(
                        onClick = onBulkMarkPaid,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mark All", fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onAddHouse, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add House", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onEditFloor, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Floor", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (floorWithHouses.isExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand/Collapse",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Progress bar (always visible)
            if (totalCount > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = AppColors.Collected,
                    trackColor = AppColors.PendingLight
                )
            }

            // Houses list (when expanded)
            if (floorWithHouses.isExpanded) {
                HorizontalDivider()
                if (floorWithHouses.houses.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No houses on this floor",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    floorWithHouses.houses.forEach { hwp ->
                        HouseRow(
                            houseWithPayment = hwp,
                            onClick = {
                                navController.navigate(
                                    Screen.HouseDetail.createRoute(
                                        buildingId,
                                        floor.floorId,
                                        hwp.house.houseId
                                    )
                                )
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HouseRow(houseWithPayment: HouseWithPayment, onClick: () -> Unit) {
    val h = houseWithPayment.house
    val isPaid = houseWithPayment.isPaid
    val borderColor = if (isPaid) AppColors.CollectedBadge else AppColors.PendingBadge

    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(borderColor)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("House ${h.houseNumber}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        h.rentAmount.toRupees() + "/month",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (isPaid && houseWithPayment.payment != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            houseWithPayment.payment.amount.toRupees(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Collected
                        )
                        Text(
                            houseWithPayment.payment.paymentMode,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    Text(
                        "PENDING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.PendingBadge
                    )
                }
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
