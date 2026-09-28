package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryRed
import com.example.ui.viewmodel.NavScreen
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.ReportPeriod

@Composable
fun DashboardScreen(
    viewModel: PosViewModel,
    onNavigate: (NavScreen) -> Unit
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val currentOpenShift by viewModel.currentOpenShift.collectAsState()
    val activeKitchenOrders by viewModel.activeKitchenOrders.collectAsState()
    val currentStaff by viewModel.currentStaff.collectAsState()
    val settings by viewModel.storeSettings.collectAsState()

    // Today's metrics
    val todayTransactions = viewModel.getFilteredTransactions(ReportPeriod.TODAY, allTransactions)
    val todayRevenue = todayTransactions.filter { it.paymentStatus == "PAID" }.sumOf { it.total }
    val todayCount = todayTransactions.filter { it.paymentStatus == "PAID" }.size
    val todayDiscount = todayTransactions.filter { it.paymentStatus == "PAID" }.sumOf { it.discountAmount }
    val todayTax = todayTransactions.filter { it.paymentStatus == "PAID" }.sumOf { it.taxAmount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Hero Store & Staff Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = settings.storeName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kasir Aktif: ${currentStaff?.name ?: "Kasir"} (${currentStaff?.role ?: "STAFF"})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF69F0AE))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "100% Offline-First Mode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Button(
                        onClick = { onNavigate(NavScreen.CASHIER) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka Kasir", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Quick Action Shortcuts
        item {
            Text(
                text = "Menu Cepat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    QuickActionChip(icon = Icons.Default.PointOfSale, label = "Kasir", color = PrimaryRed) {
                        onNavigate(NavScreen.CASHIER)
                    }
                }
                item {
                    QuickActionChip(icon = Icons.Default.SoupKitchen, label = "Dapur (${activeKitchenOrders.size})", color = AccentOrange) {
                        onNavigate(NavScreen.KITCHEN)
                    }
                }
                item {
                    QuickActionChip(icon = Icons.Default.AccountBalanceWallet, label = if (currentOpenShift != null) "Kas Buka" else "Kas Tutup", color = AccentGreen) {
                        onNavigate(NavScreen.CASH)
                    }
                }
                item {
                    QuickActionChip(icon = Icons.Default.Warehouse, label = "Stok", color = Color(0xFF1976D2)) {
                        onNavigate(NavScreen.STOCK)
                    }
                }
                item {
                    QuickActionChip(icon = Icons.Default.Analytics, label = "Laporan", color = AccentGold) {
                        onNavigate(NavScreen.REPORTS)
                    }
                }
            }
        }

        // Today's Sales Key Metrics
        item {
            Text(
                text = "Ringkasan Penjualan Hari Ini",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Total Omset",
                    value = formatRp(todayRevenue),
                    icon = Icons.Default.Payments,
                    color = PrimaryRed,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Transaksi",
                    value = "$todayCount Struk",
                    icon = Icons.Default.ReceiptLong,
                    color = AccentGreen,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Total Diskon",
                    value = formatRp(todayDiscount),
                    icon = Icons.Default.Discount,
                    color = AccentOrange,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Pajak",
                    value = formatRp(todayTax),
                    icon = Icons.Default.AccountBalance,
                    color = Color(0xFF0097A7),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Low Stock Warning Section
        if (lowStockProducts.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AccentOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Peringatan Stok Menipis (${lowStockProducts.size})",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                            TextButton(onClick = { onNavigate(NavScreen.STOCK) }) {
                                Text("Kelola Stok", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        lowStockProducts.take(3).forEach { prod ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = prod.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF3E2723)
                                )
                                StatusBadge(
                                    text = "Sisa ${prod.stock.toInt()} ${prod.unit}",
                                    color = if (prod.stock <= 0) Color.Red else AccentOrange
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Orders list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaksi Terakhir",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onNavigate(NavScreen.TRANSACTIONS) }) {
                    Text("Lihat Semua")
                }
            }
        }

        if (allTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada transaksi hari ini. Buka Kasir untuk memulai penjualan!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(allTransactions.take(5)) { tx ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.loadTransactionForReceipt(tx)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = tx.transactionNo,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${tx.customerName} • ${tx.paymentMethodsSummary}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatRp(tx.total),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            StatusBadge(
                                text = tx.paymentStatus,
                                color = when (tx.paymentStatus) {
                                    "PAID" -> AccentGreen
                                    "REFUNDED" -> AccentOrange
                                    else -> Color.Red
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}
