package com.example.ui.screens.transactions

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Transaction
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatDateTime
import com.example.ui.components.formatRp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryRed
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val transactions by viewModel.allTransactions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterStatus by remember { mutableStateOf("ALL") } // ALL, PAID, REFUNDED, VOID
    var selectedTxForAction by remember { mutableStateOf<Transaction?>(null) }
    var showRefundVoidDialog by remember { mutableStateOf<Pair<Transaction, String>?>(null) } // Pair(tx, "REFUND" or "VOID")

    val filtered = transactions.filter { tx ->
        val matchesStatus = selectedFilterStatus == "ALL" || tx.paymentStatus == selectedFilterStatus
        val matchesQuery = searchQuery.isBlank() ||
                tx.transactionNo.contains(searchQuery, ignoreCase = true) ||
                tx.customerName.contains(searchQuery, ignoreCase = true) ||
                tx.customerPhone.contains(searchQuery) ||
                tx.cashierName.contains(searchQuery, ignoreCase = true) ||
                tx.paymentMethodsSummary.contains(searchQuery, ignoreCase = true)
        matchesStatus && matchesQuery
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
            Text(
                text = "Riwayat Transaksi (${filtered.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = { viewModel.exportTransactionsCsv(context, filtered) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export CSV")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Cari no struk, kasir, pelanggan, metode bayar...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedFilterStatus == "ALL",
                onClick = { selectedFilterStatus = "ALL" },
                label = { Text("Semua") }
            )
            FilterChip(
                selected = selectedFilterStatus == "PAID",
                onClick = { selectedFilterStatus = "PAID" },
                label = { Text("Lunas") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentGreen,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedFilterStatus == "REFUNDED",
                onClick = { selectedFilterStatus = "REFUNDED" },
                label = { Text("Refund") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentOrange,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedFilterStatus == "VOID",
                onClick = { selectedFilterStatus = "VOID" },
                label = { Text("Void") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryRed,
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Tidak ada data transaksi yang cocok", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 30.dp)
            ) {
                items(filtered) { tx ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tx.transactionNo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                StatusBadge(
                                    text = tx.paymentStatus,
                                    color = when (tx.paymentStatus) {
                                        "PAID" -> AccentGreen
                                        "REFUNDED" -> AccentOrange
                                        else -> PrimaryRed
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${formatDateTime(tx.timestamp)} • Kasir: ${tx.cashierName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (tx.customerName.isNotBlank() && tx.customerName != "Pelanggan Umum") {
                                Text(
                                    text = "Pelanggan: ${tx.customerName} (${tx.customerPhone.ifBlank { "No HP -" }})",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (tx.tableNo.isNotBlank()) {
                                Text(
                                    text = "No Meja: ${tx.tableNo}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tx.paymentMethodsSummary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = formatRp(tx.total),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 10.dp))

                            // Action buttons: Cetak Ulang, Refund, Void
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.loadTransactionForReceipt(tx) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cetak Struk", fontSize = 12.sp)
                                }

                                if (tx.paymentStatus == "PAID") {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = { showRefundVoidDialog = Pair(tx, "REFUND") },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Refund", color = AccentOrange, fontSize = 12.sp)
                                    }
                                    TextButton(
                                        onClick = { showRefundVoidDialog = Pair(tx, "VOID") },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Void", color = PrimaryRed, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Refund / Void confirmation dialog
    if (showRefundVoidDialog != null) {
        val (tx, actionType) = showRefundVoidDialog!!
        var reason by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showRefundVoidDialog = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(0.92f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = if (actionType == "REFUND") "Konfirmasi Refund Transaksi" else "Konfirmasi Void Transaksi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (actionType == "REFUND") AccentOrange else PrimaryRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Struk #${tx.transactionNo} senilai ${formatRp(tx.total)}. Stok barang akan otomatis dikembalikan ke inventori.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Alasan $actionType *") },
                        placeholder = { Text("Misal: Pesanan dibatalkan / salah input") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showRefundVoidDialog = null }) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (reason.isNotBlank()) {
                                    if (actionType == "REFUND") {
                                        viewModel.refundTransaction(tx, reason)
                                    } else {
                                        viewModel.voidTransaction(tx, reason)
                                    }
                                    showRefundVoidDialog = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (actionType == "REFUND") AccentOrange else PrimaryRed
                            )
                        ) {
                            Text("Konfirmasi $actionType")
                        }
                    }
                }
            }
        }
    }
}
