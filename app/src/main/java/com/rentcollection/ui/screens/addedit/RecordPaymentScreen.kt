package com.rentcollection.ui.screens.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rentcollection.ui.components.LoadingScreen
import com.rentcollection.ui.screens.dashboard.MonthYearPickerDialog
import com.rentcollection.utils.toFullMonthName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(
    navController: NavController,
    houseId: String,
    buildingId: String,
    floorId: String,
    viewModel: RecordPaymentViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showMonthPicker by remember { mutableStateOf(false) }
    var paymentModeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) navController.popBackStack()
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            currentMonth = state.month,
            currentYear = state.year,
            onDismiss = { showMonthPicker = false },
            onConfirm = { m, y -> viewModel.onMonthChange(m, y); showMonthPicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Payment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (state.isLoading) { LoadingScreen(Modifier.padding(padding)); return@Scaffold }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Context header
            if (state.houseNumber.isNotEmpty() || state.buildingName.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (state.buildingName.isNotEmpty()) {
                            Text(state.buildingName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        if (state.houseNumber.isNotEmpty()) {
                            Text("House ${state.houseNumber}", fontSize = 13.sp)
                        }
                        if (state.tenantName.isNotEmpty()) {
                            Text("Tenant: ${state.tenantName}", fontSize = 13.sp)
                        }
                        if (state.rentAmount > 0) {
                            Text(
                                "Standard Rent: ₹${state.rentAmount}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Month
            OutlinedButton(
                onClick = { showMonthPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Payment for: ${state.month.toFullMonthName()} ${state.year}",
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }

            // Amount
            OutlinedTextField(
                value = state.amount,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Amount (₹) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("₹ ") },
                isError = state.error != null && (state.amount.toDoubleOrNull() ?: 0.0) <= 0
            )

            // Payment Mode dropdown
            ExposedDropdownMenuBox(
                expanded = paymentModeExpanded,
                onExpandedChange = { paymentModeExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.paymentMode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Mode") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentModeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = paymentModeExpanded,
                    onDismissRequest = { paymentModeExpanded = false }
                ) {
                    PAYMENT_MODES.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode) },
                            onClick = { viewModel.onPaymentModeChange(mode); paymentModeExpanded = false }
                        )
                    }
                }
            }

            // Paid date
            OutlinedTextField(
                value = state.paidDate,
                onValueChange = viewModel::onPaidDateChange,
                label = { Text("Payment Date  (dd/MM/yyyy)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
            )

            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Is Received toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mark as Received", fontWeight = FontWeight.Medium)
                    Text(
                        "Toggle off to record as pending",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Switch(checked = state.isReceived, onCheckedChange = viewModel::onIsReceivedChange)
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isLoading,
                colors = if (state.isReceived)
                    ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                else ButtonDefaults.buttonColors()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(
                        if (state.isReceived) Icons.Default.CheckCircle else Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isReceived) "Save as Received" else "Save as Pending")
                }
            }
        }
    }
}
