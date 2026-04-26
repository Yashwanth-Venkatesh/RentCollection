package com.rentcollection.ui.screens.house

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rentcollection.data.model.Payment
import com.rentcollection.ui.components.LoadingScreen
import com.rentcollection.ui.navigation.Screen
import com.rentcollection.ui.screens.dashboard.MonthYearPickerDialog
import com.rentcollection.ui.theme.AppColors
import com.rentcollection.utils.toFullMonthName
import com.rentcollection.utils.toMonthName
import com.rentcollection.utils.toRupees

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseDetailScreen(
    navController: NavController,
    buildingId: String,
    floorId: String,
    houseId: String,
    viewModel: HouseDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
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
                title = {
                    Text(
                        "House ${state.house?.houseNumber ?: ""}",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(
                            Screen.AddEditHouse.createRoute(buildingId, floorId, houseId)
                        )
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit House")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(
                        Screen.RecordPayment.createRoute(houseId, buildingId, floorId)
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Record Payment", tint = Color.White)
            }
        }
    ) { padding ->
        if (state.isLoading) { LoadingScreen(Modifier.padding(padding)); return@Scaffold }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // House info
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("House Info", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        InfoRow("Building", state.buildingName)
                        InfoRow("House Number", state.house?.houseNumber ?: "-")
                        InfoRow("Rent Amount", state.house?.rentAmount?.toRupees() ?: "-")
                        if ((state.house?.depositAmount ?: 0.0) > 0) {
                            InfoRow("Deposit", state.house?.depositAmount?.toRupees() ?: "-")
                        }
                    }
                }
            }

            // Tenant info
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tenant", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = {
                                navController.navigate(
                                    Screen.AddEditTenant.createRoute(
                                        houseId, buildingId,
                                        state.tenant?.tenantId
                                    )
                                )
                            }) {
                                Text(if (state.tenant != null) "Edit" else "Add Tenant")
                            }
                        }
                        if (state.tenant != null) {
                            val t = state.tenant!!
                            InfoRow("Name", t.name)
                            InfoRow("Phone", t.phone)
                            if (t.email.isNotEmpty()) InfoRow("Email", t.email)
                            if (t.alternatePhone.isNotEmpty()) InfoRow("Alt Phone", t.alternatePhone)
                            if (t.moveInDate.isNotEmpty()) InfoRow("Move-in", t.moveInDate)
                            if (t.notes.isNotEmpty()) InfoRow("Notes", t.notes)
                            Spacer(Modifier.height(8.dp))
                            if (t.phone.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${t.phone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Call ${t.name}")
                                }
                            }
                        } else {
                            Text(
                                "No tenant assigned",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // Current month payment status
            item {
                OutlinedButton(
                    onClick = { showMonthPicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Viewing: ${state.selectedMonth.toFullMonthName()} ${state.selectedYear}",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }

            item {
                val payment = state.currentMonthPayment
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${state.selectedMonth.toFullMonthName()} ${state.selectedYear} Status",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            if (payment?.isReceived == true) {
                                Badge(containerColor = Color(0xFF4CAF50)) {
                                    Text("PAID", fontSize = 10.sp)
                                }
                            } else {
                                Badge(containerColor = Color(0xFFEF5350)) {
                                    Text("PENDING", fontSize = 10.sp)
                                }
                            }
                        }
                        if (payment != null) {
                            Spacer(Modifier.height(8.dp))
                            InfoRow("Amount", payment.amount.toRupees())
                            InfoRow("Mode", payment.paymentMode)
                            if (payment.recordedBy.isNotEmpty()) InfoRow("Recorded by", payment.recordedBy)
                            if (payment.notes.isNotEmpty()) InfoRow("Notes", payment.notes)
                            if (payment.paidDate.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Paid on ${payment.paidDate}", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = AppColors.CollectedLight,
                                        labelColor = AppColors.Collected,
                                        leadingIconContentColor = AppColors.Collected
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                navController.navigate(
                                    Screen.RecordPayment.createRoute(houseId, buildingId, floorId)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (payment?.isReceived == true)
                                ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            else ButtonDefaults.buttonColors()
                        ) {
                            Icon(
                                if (payment?.isReceived == true) Icons.Default.Edit else Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (payment?.isReceived == true) "Update Payment" else "Mark as Received")
                        }
                    }
                }
            }

            // Payment history
            if (state.paymentHistory.isNotEmpty()) {
                item {
                    Text("Payment History", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
                items(state.paymentHistory) { p ->
                    PaymentHistoryRow(p)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PaymentHistoryRow(payment: Payment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (payment.isReceived) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (payment.isReceived) Color(0xFF4CAF50) else Color(0xFFFF8F00),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${payment.month.toMonthName()} ${payment.year}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    payment.paymentMode + if (payment.recordedBy.isNotEmpty()) " · by ${payment.recordedBy}" else "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(
                payment.amount.toRupees(),
                fontWeight = FontWeight.Bold,
                color = if (payment.isReceived) Color(0xFF2E7D32) else Color(0xFFFF8F00),
                fontSize = 14.sp
            )
        }
    }
}
