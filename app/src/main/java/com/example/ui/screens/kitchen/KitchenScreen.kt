package com.example.ui.screens.kitchen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatTime
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryRed
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.delay

@Composable
fun KitchenScreen(
    viewModel: PosViewModel
) {
    val activeKitchenOrders by viewModel.activeKitchenOrders.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, BARU, DIPROSES, SIAP

    // Live timer tick to update elapsed minutes
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000) // update every 10 seconds
            currentTime = System.currentTimeMillis()
        }
    }

    val displayOrders = if (selectedFilter == "ALL") {
        activeKitchenOrders
    } else {
        activeKitchenOrders.filter { it.kitchenStatus == selectedFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Kitchen Display System (KDS)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pesanan Dapur Aktif (${activeKitchenOrders.size})",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status Tabs
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("Semua (${activeKitchenOrders.size})") }
                )
            }
            item {
                val c = activeKitchenOrders.count { it.kitchenStatus == "BARU" }
                FilterChip(
                    selected = selectedFilter == "BARU",
                    onClick = { selectedFilter = "BARU" },
                    label = { Text("Baru ($c)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                val c = activeKitchenOrders.count { it.kitchenStatus == "DIPROSES" }
                FilterChip(
                    selected = selectedFilter == "DIPROSES",
                    onClick = { selectedFilter = "DIPROSES" },
                    label = { Text("Diproses ($c)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                val c = activeKitchenOrders.count { it.kitchenStatus == "SIAP" }
                FilterChip(
                    selected = selectedFilter == "SIAP",
                    onClick = { selectedFilter = "SIAP" },
                    label = { Text("Siap Saji ($c)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (displayOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SoupKitchen,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Semua pesanan dapur sudah selesai!",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Pesanan baru dari kasir akan otomatis muncul di sini",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 30.dp)
            ) {
                items(displayOrders, key = { it.id }) { tx ->
                    KitchenTicketCard(
                        transaction = tx,
                        currentTime = currentTime,
                        onStatusChange = { newStatus ->
                            viewModel.updateKitchenStatus(tx.id, newStatus)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KitchenTicketCard(
    transaction: Transaction,
    currentTime: Long,
    onStatusChange: (String) -> Unit
) {
    val elapsedMinutes = ((currentTime - transaction.timestamp) / 60000).coerceAtLeast(0)
    val timerColor = when {
        elapsedMinutes >= 15 -> Color.Red
        elapsedMinutes >= 8 -> AccentOrange
        else -> AccentGreen
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Order No, Table, Timer, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = transaction.transactionNo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (transaction.tableNo.isNotBlank()) "Meja: ${transaction.tableNo} • ${transaction.customerName}" else transaction.customerName,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    StatusBadge(
                        text = transaction.kitchenStatus,
                        color = when (transaction.kitchenStatus) {
                            "BARU" -> PrimaryRed
                            "DIPROSES" -> AccentOrange
                            "SIAP" -> AccentGreen
                            else -> Color.Gray
                        }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = timerColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${elapsedMinutes}m lalu (${formatTime(transaction.timestamp)})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = timerColor
                        )
                    }
                }
            }

            if (transaction.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Catatan: ${transaction.notes}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryRed,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (transaction.kitchenStatus) {
                    "BARU" -> {
                        Button(
                            onClick = { onStatusChange("DIPROSES") },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.SoupKitchen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mulai Masak", fontWeight = FontWeight.Bold)
                        }
                    }
                    "DIPROSES" -> {
                        Button(
                            onClick = { onStatusChange("SIAP") },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Siap Disajikan", fontWeight = FontWeight.Bold)
                        }
                    }
                    "SIAP" -> {
                        Button(
                            onClick = { onStatusChange("SELESAI") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Selesai & Antar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
