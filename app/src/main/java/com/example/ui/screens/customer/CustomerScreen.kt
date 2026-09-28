package com.example.ui.screens.customer

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
import com.example.data.model.Customer
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRp
import com.example.ui.theme.AccentGreen
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val customers by viewModel.allCustomers.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }

    val filtered = customers.filter { c ->
        searchQuery.isBlank() ||
                c.name.contains(searchQuery, ignoreCase = true) ||
                c.phone.contains(searchQuery) ||
                c.address.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingNew = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Pelanggan")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Data Pelanggan (${customers.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = { viewModel.exportCustomersCsv(context) },
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
                placeholder = { Text("Cari nama, no HP (08/628/+628)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tidak ada pelanggan yang cocok", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filtered) { cust ->
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
                                    Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Telp: ${cust.phone}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    if (cust.address.isNotBlank()) {
                                        Text("Alamat: ${cust.address}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (cust.notes.isNotBlank()) {
                                        Text("Catatan: ${cust.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StatusBadge(text = "${cust.totalTransactions} Transaksi", color = MaterialTheme.colorScheme.primary)
                                        StatusBadge(text = formatRp(cust.totalSpent), color = AccentGreen)
                                        if (cust.discountPercent > 0) {
                                            StatusBadge(text = "Diskon: ${cust.discountPercent.toInt()}%", color = Color(0xFFE65100))
                                        }
                                    }
                                }

                                Row {
                                    IconButton(onClick = { editingCustomer = cust }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { viewModel.deleteCustomer(cust) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isAddingNew || editingCustomer != null) {
        val custToEdit = editingCustomer
        var name by remember { mutableStateOf(custToEdit?.name ?: "") }
        var phone by remember { mutableStateOf(custToEdit?.phone ?: "") }
        var address by remember { mutableStateOf(custToEdit?.address ?: "") }
        var notes by remember { mutableStateOf(custToEdit?.notes ?: "") }
        var discountPct by remember { mutableStateOf(custToEdit?.discountPercent?.toInt()?.toString() ?: "0") }

        Dialog(onDismissRequest = { isAddingNew = false; editingCustomer = null }) {
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
                        text = if (custToEdit == null) "Tambah Pelanggan Baru" else "Edit Data Pelanggan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Pelanggan *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Nomor HP (08xx / 628xx / +628xx) *") },
                        placeholder = { Text("081234567890") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Alamat") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = discountPct,
                        onValueChange = { discountPct = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Diskon Khusus Pelanggan (%)") },
                        placeholder = { Text("0") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan / Info Member") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isAddingNew = false; editingCustomer = null }) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank() && phone.isNotBlank()) {
                                    val c = Customer(
                                        id = custToEdit?.id ?: 0L,
                                        name = name.trim(),
                                        phone = phone.trim(),
                                        address = address.trim(),
                                        notes = notes.trim(),
                                        discountPercent = discountPct.toDoubleOrNull() ?: 0.0,
                                        totalSpent = custToEdit?.totalSpent ?: 0.0,
                                        totalTransactions = custToEdit?.totalTransactions ?: 0
                                    )
                                    viewModel.saveCustomer(c)
                                    isAddingNew = false
                                    editingCustomer = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}
