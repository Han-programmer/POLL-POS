package com.example.ui.screens.reports

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.export.DataExportHelper
import com.example.ui.components.MetricCard
import com.example.ui.components.formatRp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryRed
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.ReportPeriod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val settings by viewModel.storeSettings.collectAsState()
    val selectedPeriod by viewModel.reportPeriod.collectAsState()

    val filteredTransactions = viewModel.getFilteredTransactions(selectedPeriod, allTransactions)
    val paidTransactions = filteredTransactions.filter { it.paymentStatus == "PAID" }

    val totalRevenue = paidTransactions.sumOf { it.total }
    val totalTransactionsCount = paidTransactions.size
    val totalDiscount = paidTransactions.sumOf { it.discountAmount }
    val totalTax = paidTransactions.sumOf { it.taxAmount }
    val avgTicket = if (totalTransactionsCount > 0) totalRevenue / totalTransactionsCount else 0.0

    // Payment method breakdown
    val paymentMap = mutableMapOf<String, Double>()
    for (tx in paidTransactions) {
        val splits = tx.paymentMethodsSummary.split(",")
        for (s in splits) {
            val parts = s.trim().split(":")
            if (parts.size >= 2) {
                val method = parts[0].trim()
                val amtStr = parts[1].replace("Rp", "").replace(".", "").trim()
                val amt = amtStr.toDoubleOrNull() ?: 0.0
                paymentMap[method] = (paymentMap[method] ?: 0.0) + amt
            } else {
                paymentMap[tx.paymentMethodsSummary.ifBlank { "Lainnya" }] =
                    (paymentMap[tx.paymentMethodsSummary.ifBlank { "Lainnya" }] ?: 0.0) + tx.total
            }
        }
    }

    val periodLabel = when (selectedPeriod) {
        ReportPeriod.TODAY -> "Hari Ini"
        ReportPeriod.YESTERDAY -> "Kemarin"
        ReportPeriod.THIS_WEEK -> "Minggu Ini"
        ReportPeriod.THIS_MONTH -> "Bulan Ini"
        ReportPeriod.THIS_YEAR -> "Tahun Ini"
        ReportPeriod.ALL_TIME -> "Semua Waktu"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Title & Export
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Laporan Penjualan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        viewModel.exportTransactionsCsv(context, paidTransactions)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excel CSV", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val html = DataExportHelper.buildSalesHtmlReport(
                            storeName = settings.storeName,
                            periodTitle = periodLabel,
                            transactions = paidTransactions,
                            totalRevenue = totalRevenue,
                            totalTransactions = totalTransactionsCount,
                            totalDiscount = totalDiscount,
                            totalTax = totalTax
                        )
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Laporan Penjualan $periodLabel - ${settings.storeName}")
                            putExtra(android.content.Intent.EXTRA_TEXT, "LAPORAN PENJUALAN ($periodLabel)\n${settings.storeName}\n\nTotal Omset: ${formatRp(totalRevenue)}\nTotal Transaksi: $totalTransactionsCount\nTotal Diskon: ${formatRp(totalDiscount)}\nTotal Pajak: ${formatRp(totalTax)}")
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, "Bagikan Ringkasan Laporan"))
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bagikan", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Period Filter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ReportPeriod.values()) { period ->
                val label = when (period) {
                    ReportPeriod.TODAY -> "Hari Ini"
                    ReportPeriod.YESTERDAY -> "Kemarin"
                    ReportPeriod.THIS_WEEK -> "Minggu Ini"
                    ReportPeriod.THIS_MONTH -> "Bulan Ini"
                    ReportPeriod.THIS_YEAR -> "Tahun Ini"
                    ReportPeriod.ALL_TIME -> "Semua"
                }
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { viewModel.setReportPeriod(period) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            // Metrics Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Total Omset ($periodLabel)",
                        value = formatRp(totalRevenue),
                        icon = Icons.Default.MonetizationOn,
                        color = PrimaryRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Total Transaksi",
                        value = "$totalTransactionsCount Struk",
                        icon = Icons.Default.Receipt,
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
                        title = "Rata-rata / Struk",
                        value = formatRp(avgTicket),
                        icon = Icons.Default.ShowChart,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Diskon Diberikan",
                        value = formatRp(totalDiscount),
                        icon = Icons.Default.Discount,
                        color = AccentOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Payment Method Breakdown
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Metode Pembayaran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (paymentMap.isEmpty()) {
                            Text("Belum ada data pembayaran", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            paymentMap.entries.sortedByDescending { it.value }.forEach { (method, amt) ->
                                val pct = if (totalRevenue > 0) (amt / totalRevenue).toFloat() else 0f
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(method, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("${formatRp(amt)} (${(pct * 100).toInt()}%)", fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { pct.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Products
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Katalog Produk & Estimasi Penjualan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        allProducts.take(5).forEachIndexed { idx, p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${idx + 1}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Column {
                                        Text(p.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("Harga: ${formatRp(p.price)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text("Sisa: ${p.stock.toInt()} ${p.unit}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            if (idx < 4) Divider(modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}
