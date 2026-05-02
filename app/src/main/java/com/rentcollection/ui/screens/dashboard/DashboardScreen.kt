@file:OptIn(ExperimentalMaterial3Api::class)

package com.rentcollection.ui.screens.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rentcollection.data.model.Building
import com.rentcollection.data.model.Payment
import com.rentcollection.ui.components.ShimmerDashboard
import com.rentcollection.ui.navigation.Screen
import com.rentcollection.ui.theme.AppColors
import com.rentcollection.ui.theme.LocalDarkTheme
import com.rentcollection.ui.theme.LocalThemeToggle
import com.rentcollection.utils.toFullMonthName
import com.rentcollection.utils.toRupees
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showMonthPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Error snackbar
    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    // Undo snackbar
    LaunchedEffect(state.undoPayment) {
        val p = state.undoPayment
        if (p != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Marked as paid",
                actionLabel = "UNDO",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoMarkAsPaid()
            } else {
                viewModel.clearUndo()
            }
        }
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            currentMonth = state.selectedMonth,
            currentYear = state.selectedYear,
            onDismiss = { showMonthPicker = false },
            onConfirm = { m, y -> viewModel.setMonth(m, y); showMonthPicker = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rent Manager", fontWeight = FontWeight.Bold)
                        if (state.userName.isNotEmpty()) {
                            Text(
                                "${state.userName} · ${state.userRole}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                            )
                        }
                    }
                },
                actions = {
                    val isDark = LocalDarkTheme.current
                    val toggleTheme = LocalThemeToggle.current
                    IconButton(onClick = toggleTheme) {
                        Icon(
                            if (isDark) Icons.Default.WbSunny else Icons.Default.DarkMode,
                            contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode"
                        )
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            ShimmerDashboard(Modifier.padding(padding))
            return@Scaffold
        }

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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Greeting
                item {
                    GreetingCard(userName = state.userName)
                }

                // Month selector
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

                // Building filter chips
                if (state.buildings.isNotEmpty()) {
                    item {
                        BuildingFilterRow(
                            buildings = state.buildings,
                            selectedBuildingId = state.selectedBuildingId,
                            onSelect = { viewModel.setBuilding(it) }
                        )
                    }
                }

                // Summary donut chart
                item {
                    SummaryDonutCard(
                        collected = state.totalCollected,
                        pending = state.totalPending,
                        percentage = state.collectionPercentage
                    )
                }

                // Trend chart (if data available and more than 1 month has activity)
                if (state.trendData.any { it.total > 0 }) {
                    item {
                        TrendChart(trendData = state.trendData)
                    }
                }

                // Pending rents header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Pending Rents", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        if (state.pendingPayments.isNotEmpty()) {
                            Surface(color = AppColors.Pending, shape = CircleShape) {
                                Text(
                                    "${state.pendingPayments.size}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // All caught up (animated)
                if (state.pendingPayments.isEmpty()) {
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) + fadeIn()
                        ) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = AppColors.Collected,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "All rents collected!",
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppColors.Collected,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            "No pending payments for ${state.selectedMonth.toFullMonthName()} ${state.selectedYear}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(
                        state.pendingPayments,
                        key = { if (it.paymentId.isNotEmpty()) it.paymentId else "h_${it.houseId}" }
                    ) { payment ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.StartToEnd) {
                                    viewModel.markAsPaid(payment)
                                    true
                                } else false
                            },
                            positionalThreshold = { it * 0.4f }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromEndToStart = false,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                                        AppColors.Collected else Color.Transparent,
                                    label = "swipe_bg"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, shape = MaterialTheme.shapes.medium)
                                        .padding(start = 20.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Icon(Icons.Default.Check, tint = Color.White, contentDescription = null)
                                }
                            }
                        ) {
                            PendingRentCard(
                                payment = payment,
                                onMarkAsPaid = { viewModel.markAsPaid(payment) },
                                onWhatsApp = {
                                    if (payment.tenantPhone.isNotEmpty()) {
                                        val msg = "Dear ${payment.tenantName}, your rent of ${payment.amount.toRupees()} for ${payment.month.toFullMonthName()} ${payment.year} is due. Please make payment at the earliest."
                                        val url = "https://wa.me/91${payment.tenantPhone.filter { it.isDigit() }}?text=${Uri.encode(msg)}"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                },
                                onCall = {
                                    if (payment.tenantPhone.isNotEmpty()) {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${payment.tenantPhone}"))
                                        )
                                    }
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
private fun GreetingCard(userName: String) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val dateStr = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()) }
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            if (userName.isNotEmpty()) "$greeting, $userName!" else "$greeting!",
            fontWeight = FontWeight.Bold, fontSize = 20.sp
        )
        Text(dateStr, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
    }
}

@Composable
private fun BuildingFilterRow(
    buildings: List<Building>,
    selectedBuildingId: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = selectedBuildingId == null, onClick = { onSelect(null) }, label = { Text("All") })
        buildings.forEach { building ->
            FilterChip(
                selected = selectedBuildingId == building.buildingId,
                onClick = { onSelect(building.buildingId) },
                label = { Text(building.name) }
            )
        }
    }
}

@Composable
private fun SummaryDonutCard(collected: Double, pending: Double, percentage: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                DonutChart(collected = collected, pending = pending, modifier = Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${(percentage * 100).toInt()}%",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = if (percentage >= 1f) AppColors.Collected else MaterialTheme.colorScheme.onSurface
                    )
                    Text("collected", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendRow(label = "Collected", amount = collected, color = AppColors.Collected)
                LegendRow(label = "Pending", amount = pending, color = AppColors.Pending)
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, amount: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, shape = CircleShape))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(amount.toRupees(), fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
        }
    }
}

@Composable
private fun DonutChart(collected: Double, pending: Double, modifier: Modifier = Modifier) {
    val total = collected + pending
    Canvas(modifier = modifier) {
        val stroke = 26.dp.toPx()
        val inset = stroke / 2f
        val arcTopLeft = Offset(inset, inset)
        val arcSize = Size(size.width - stroke, size.height - stroke)
        if (total <= 0.0) {
            drawArc(
                color = AppColors.ChartTrack,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )
        } else {
            val collectedSweep = (collected / total * 360).toFloat()
            val pendingSweep = 360f - collectedSweep
            drawArc(
                color = AppColors.ChartTrack,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )
            if (collectedSweep > 1f) {
                drawArc(
                    color = AppColors.Collected,
                    startAngle = -90f,
                    sweepAngle = collectedSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            if (pendingSweep > 1f) {
                drawArc(
                    color = AppColors.Pending,
                    startAngle = -90f + collectedSweep,
                    sweepAngle = pendingSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
private fun TrendChart(trendData: List<MonthlyTrend>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("6-Month Collection Trend", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            val collectedColor = AppColors.Collected
            val trackColor = AppColors.ChartTrack
            val maxTotal = trendData.maxOfOrNull { it.total } ?: 1.0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                trendData.forEach { trend ->
                    val frac = if (maxTotal > 0 && trend.total > 0) (trend.collected / maxTotal).toFloat() else 0f
                    val totalFrac = if (maxTotal > 0) (trend.total / maxTotal).toFloat() else 0f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Canvas(modifier = Modifier.weight(1f).fillMaxWidth(0.6f)) {
                            val barW = size.width
                            val totalH = size.height * totalFrac
                            val collectedH = size.height * frac
                            val radius = CornerRadius(4.dp.toPx())
                            if (totalH > 0) {
                                drawRoundRect(
                                    color = trackColor,
                                    topLeft = Offset(0f, size.height - totalH),
                                    size = Size(barW, totalH),
                                    cornerRadius = radius
                                )
                            }
                            if (collectedH > 0) {
                                drawRoundRect(
                                    color = collectedColor,
                                    topLeft = Offset(0f, size.height - collectedH),
                                    size = Size(barW, collectedH),
                                    cornerRadius = radius
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            trend.label,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingRentCard(
    payment: Payment,
    onMarkAsPaid: () -> Unit,
    onWhatsApp: () -> Unit = {},
    onCall: () -> Unit = {}
) {
    val locationParts = buildList {
        if (payment.buildingName.isNotEmpty()) add(payment.buildingName)
        if (payment.floorName.isNotEmpty()) add(payment.floorName)
        if (payment.houseNumber.isNotEmpty()) add("House ${payment.houseNumber}")
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(AppColors.Pending))
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
                if (locationParts.isNotEmpty()) {
                    Text(
                        locationParts.joinToString(" · "),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
                Text(
                    payment.tenantName.ifEmpty { "No tenant assigned" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                // Phone action buttons
                if (payment.tenantPhone.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCall,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Call", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onWhatsApp,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 11.sp)
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.padding(end = 12.dp, top = 10.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    payment.amount.toRupees(),
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Pending,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onMarkAsPaid,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Collected)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Mark Paid", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun MonthYearPickerDialog(
    currentMonth: Int,
    currentYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var selMonth by remember { mutableIntStateOf(currentMonth) }
    var selYear by remember { mutableIntStateOf(currentYear) }
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month & Year") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { selYear-- }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev year")
                    }
                    Text(selYear.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { selYear++ }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next year")
                    }
                }
                Spacer(Modifier.height(8.dp))
                months.chunked(4).forEachIndexed { rowIdx, rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowMonths.forEachIndexed { colIdx, name ->
                            val m = rowIdx * 4 + colIdx + 1
                            val selected = selMonth == m
                            TextButton(
                                onClick = { selMonth = m },
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) { Text(name, fontSize = 13.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selMonth, selYear) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
