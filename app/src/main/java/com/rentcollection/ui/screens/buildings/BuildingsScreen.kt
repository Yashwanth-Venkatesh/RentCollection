@file:OptIn(ExperimentalMaterial3Api::class)

package com.rentcollection.ui.screens.buildings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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

@Composable
fun BuildingsScreen(
    navController: NavController,
    viewModel: BuildingsViewModel = hiltViewModel()
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
                title = { Text("Buildings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddEditBuilding.createRoute()) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Building", tint = Color.White)
            }
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
                OutlinedButton(
                    onClick = { showMonthPicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${state.selectedMonth.toFullMonthName()} ${state.selectedYear}",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearch(it) },
                    placeholder = { Text("Search buildings…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearch("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                if (state.buildings.isEmpty()) {
                    EmptyState(
                        message = "No buildings yet.\nTap + to add one.",
                        icon = Icons.Default.Business
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.buildings) { bws ->
                            BuildingCard(
                                buildingWithStats = bws,
                                onClick = {
                                    navController.navigate(
                                        Screen.BuildingDetail.createRoute(bws.building.buildingId)
                                    )
                                },
                                onEdit = {
                                    navController.navigate(
                                        Screen.AddEditBuilding.createRoute(bws.building.buildingId)
                                    )
                                }
                            )
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
private fun BuildingCard(
    buildingWithStats: BuildingWithStats,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val b = buildingWithStats.building
    val total = buildingWithStats.collected + buildingWithStats.pending
    val progress = if (total > 0) (buildingWithStats.collected / total).toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(b.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (b.address.isNotEmpty()) {
                        Text(b.address, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Collected", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(
                        buildingWithStats.collected.toRupees(),
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Collected,
                        fontSize = 14.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Pending", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(
                        buildingWithStats.pending.toRupees(),
                        fontWeight = FontWeight.Bold,
                        color = if (buildingWithStats.pending > 0) AppColors.Pending
                                else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }
            }
            if (total > 0) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AppColors.Collected,
                    trackColor = AppColors.PendingLight
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(progress * 100).toInt()}% collected",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}
