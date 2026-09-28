package com.example.ui.screens.stock

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Product
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatDateTime
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryRed
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    viewModel: PosViewModel
) {
    val allProducts by viewModel.allProducts.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val movements by viewModel.allStockMovements.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Stok Produk, 1: Riwayat Mutasi
    var filterLowStockOnly by remember { mutableStateOf(false) }
    var selectedProductForAdjust by remember { mutableStateOf<Product?>(null) }

    val displayProducts = if (filterLowStockOnly) lowStockProducts else allProducts

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Manajemen Inventori & Stok",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs: Stok Produk vs Riwayat Mutasi
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Daftar Stok") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Riwayat Mutasi (${movements.size})") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // Stock Summary Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total SKU Produk", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${allProducts.size} Produk", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (lowStockProducts.isNotEmpty()) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Stok Rendah", fontSize = 12.sp, color = if (lowStockProducts.isNotEmpty()) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${lowStockProducts.size} Produk",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = if (lowStockProducts.isNotEmpty()) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !filterLowStockOnly,
                    onClick = { filterLowStockOnly = false },
                    label = { Text("Semua Produk") }
                )
                FilterChip(
                    selected = filterLowStockOnly,
                    onClick = { filterLowStockOnly = true },
                    label = { Text("Peringatan Stok Rendah (${lowStockProducts.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Products list with stock adjustments
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 30.dp)
            ) {
                items(displayProducts) { prod ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "Min Stok: ${prod.minStock.toInt()} ${prod.unit} • SKU: ${prod.sku.ifBlank { "-" }}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                StatusBadge(
                                    text = "Sisa: ${prod.stock.toInt()} ${prod.unit}",
                                    color = if (prod.stock <= 0) Color.Red else if (prod.stock <= prod.minStock) AccentOrange else AccentGreen
                                )
                            }

                            Button(
                                onClick = { selectedProductForAdjust = prod },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text("Atur Stok", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // Stock Movements History
            if (movements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada riwayat mutasi stok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 30.dp)
                ) {
                    items(movements) { mov ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mov.productName, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${formatDateTime(mov.timestamp)} • Staf: ${mov.staffName}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (mov.note.isNotBlank()) {
                                        Text("Ket: ${mov.note}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    StatusBadge(
                                        text = when (mov.movementType) {
                                            "IN" -> "+${mov.quantity.toInt()} (Masuk)"
                                            "OUT" -> "-${mov.quantity.toInt()} (Keluar)"
                                            "SALE" -> "-${mov.quantity.toInt()} (Terjual)"
                                            "REFUND" -> "+${mov.quantity.toInt()} (Refund)"
                                            "OPNAME" -> "Opname: ${mov.newStock.toInt()}"
                                            else -> "${mov.movementType}: ${mov.quantity.toInt()}"
                                        },
                                        color = when (mov.movementType) {
                                            "IN", "REFUND" -> AccentGreen
                                            "OUT", "SALE" -> PrimaryRed
                                            else -> AccentOrange
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${mov.previousStock.toInt()} → ${mov.newStock.toInt()}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Stock Adjustment Dialog (IN, OUT, OPNAME, ADJUSTMENT)
    if (selectedProductForAdjust != null) {
        val prod = selectedProductForAdjust!!
        var adjustType by remember { mutableStateOf("IN") } // IN, OUT, OPNAME
        var qtyInput by remember { mutableStateOf("") }
        var noteInput by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { selectedProductForAdjust = null }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(0.92f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text("Penyesuaian Stok", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${prod.name} (Stok sekarang: ${prod.stock.toInt()} ${prod.unit})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = adjustType == "IN",
                            onClick = { adjustType = "IN" },
                            label = { Text("Masuk (+)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = adjustType == "OUT",
                            onClick = { adjustType = "OUT" },
                            label = { Text("Keluar (-)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = adjustType == "OPNAME",
                            onClick = { adjustType = "OPNAME" },
                            label = { Text("Opname") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { qtyInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (adjustType == "OPNAME") "Stok Riil Hasil Opname" else "Jumlah Stok") },
                        placeholder = { Text("Misal: 10") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Alasan / Keterangan") },
                        placeholder = { Text("Misal: Kulakan pasar, rusak, dll") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedProductForAdjust = null }) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val qty = qtyInput.toDoubleOrNull() ?: 0.0
                                if (qty > 0 || adjustType == "OPNAME") {
                                    viewModel.adjustStock(
                                        productId = prod.id,
                                        productName = prod.name,
                                        type = adjustType,
                                        qty = qty,
                                        note = noteInput
                                    )
                                    selectedProductForAdjust = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Simpan Stok")
                        }
                    }
                }
            }
        }
    }
}
