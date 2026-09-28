package com.example.ui.screens.cash

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
import com.example.data.model.CashShift
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatDateTime
import com.example.ui.components.formatRp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryRed
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashManagementScreen(
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val currentOpenShift by viewModel.currentOpenShift.collectAsState()
    val allShifts by viewModel.allShifts.collectAsState()

    var showOpenShiftDialog by remember { mutableStateOf(false) }
    var showCashEntryDialog by remember { mutableStateOf<String?>(null) } // "PAY_IN" or "PAY_OUT"
    var showCloseShiftDialog by remember { mutableStateOf(false) }

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
                text = "Manajemen Kas & Laci",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = { viewModel.exportCashShiftsCsv(context) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export CSV")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (currentOpenShift == null) {
            // No open shift card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Shift Kas Sedang Ditutup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Buka shift kas dengan memasukkan modal awal laci untuk mulai mencatat transaksi tunai.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showOpenShiftDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka Shift Kas (Modal Awal)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Active Shift Card
            val shift = currentOpenShift!!
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Shift Kas Aktif",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kasir: ${shift.staffName} • Dibuka: ${formatDateTime(shift.openedAt)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(text = "Shift Terbuka", color = AccentGreen)
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Modal Awal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatRp(shift.initialCash), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column {
                            Text("Penjualan Tunai", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatRp(shift.totalCashSales), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccentGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Kas Masuk (Pay In)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("+${formatRp(shift.totalPayIn)}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccentGreen)
                        }
                        Column {
                            Text("Kas Keluar (Pay Out)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("-${formatRp(shift.totalPayOut)}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PrimaryRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kas Seharusnya di Laci:", fontWeight = FontWeight.Bold)
                            Text(
                                text = formatRp(shift.expectedCash),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons: Pay In, Pay Out, Tutup Kas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCashEntryDialog = "PAY_IN" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Kas Masuk", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { showCashEntryDialog = "PAY_OUT" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("- Kas Keluar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryRed)
                        }
                        Button(
                            onClick = { showCloseShiftDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                        ) {
                            Text("Tutup Kas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History of Shifts
        Text(
            text = "Riwayat Shift Kas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            items(allShifts) { s ->
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
                            Text("Shift #${s.id} • ${s.staffName}", fontWeight = FontWeight.Bold)
                            StatusBadge(
                                text = if (s.isOpen) "Sedang Berjalan" else "Selesai",
                                color = if (s.isOpen) AccentGreen else Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Buka: ${formatDateTime(s.openedAt)}" + if (s.closedAt != null) " • Tutup: ${formatDateTime(s.closedAt)}" else "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Modal: ${formatRp(s.initialCash)}", fontSize = 12.sp)
                            Text("Penjualan Tunai: ${formatRp(s.totalCashSales)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        if (!s.isOpen && s.actualCash != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Kas Riil: ${formatRp(s.actualCash)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Selisih: ${formatRp(s.difference)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (s.difference >= 0) AccentGreen else PrimaryRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Open Shift Dialog
    if (showOpenShiftDialog) {
        var modalInput by remember { mutableStateOf("200000") }

        Dialog(onDismissRequest = { showOpenShiftDialog = false }) {
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
                    Text("Buka Shift Kas Baru", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Masukkan modal awal kas di laci kasir", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(100000.0, 200000.0, 500000.0).forEach { amt ->
                            OutlinedButton(
                                onClick = { modalInput = amt.toLong().toString() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text(formatRp(amt), fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = modalInput,
                        onValueChange = { modalInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Modal Awal (Rp) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showOpenShiftDialog = false }) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = modalInput.toDoubleOrNull() ?: 200000.0
                                viewModel.openShift(amt)
                                showOpenShiftDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Buka Shift")
                        }
                    }
                }
            }
        }
    }

    // Cash Entry Dialog (Pay In / Pay Out)
    if (showCashEntryDialog != null) {
        val type = showCashEntryDialog!!
        var amtInput by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Operasional") }
        var note by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showCashEntryDialog = null }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(0.92f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (type == "PAY_IN") "Kas Masuk (Pay In)" else "Kas Keluar (Pay Out)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (type == "PAY_IN") AccentGreen else PrimaryRed
                    )

                    OutlinedTextField(
                        value = amtInput,
                        onValueChange = { amtInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Jumlah (Rp) *") },
                        placeholder = { Text("Misal: 50000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori (misal: Beli Bahan, Operasional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan / Keterangan *") },
                        placeholder = { Text("Keterangan keperluan kas...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCashEntryDialog = null }) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = amtInput.toDoubleOrNull() ?: 0.0
                                if (amt > 0 && note.isNotBlank()) {
                                    viewModel.addCashEntry(type, amt, note, category)
                                    showCashEntryDialog = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "PAY_IN") AccentGreen else PrimaryRed
                            )
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }

    // Close Shift Dialog
    if (showCloseShiftDialog && currentOpenShift != null) {
        val shift = currentOpenShift!!
        var actualCashInput by remember { mutableStateOf(shift.expectedCash.toLong().toString()) }
        var notesInput by remember { mutableStateOf("") }

        val actualNum = actualCashInput.toDoubleOrNull() ?: 0.0
        val diff = actualNum - shift.expectedCash

        Dialog(onDismissRequest = { showCloseShiftDialog = false }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(0.94f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text("Penutupan Shift Kas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Hitung seluruh uang fisik di laci kasir", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Kas Seharusnya:")
                                Text(formatRp(shift.expectedCash), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = actualCashInput,
                        onValueChange = { actualCashInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Kas Aktual di Laci (Rp) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live difference
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Selisih Kas:", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = formatRp(diff),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (diff == 0.0) AccentGreen else if (diff > 0) Color(0xFF1976D2) else PrimaryRed
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Catatan Penutupan") },
                        placeholder = { Text("Keterangan jika ada selisih kas...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCloseShiftDialog = false }) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.closeShift(actualNum, notesInput)
                                showCloseShiftDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                        ) {
                            Text("Tutup Shift Sekarang")
                        }
                    }
                }
            }
        }
    }
}
