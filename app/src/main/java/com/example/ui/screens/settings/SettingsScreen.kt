package com.example.ui.screens.settings

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.StaffUser
import com.example.data.model.StoreSettings
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.PrimaryRed
import com.example.ui.viewmodel.PosViewModel
import com.example.util.rememberProductImagePicker
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val allStaff by viewModel.allStaff.collectAsState()
    val currentStaff by viewModel.currentStaff.collectAsState()
    val pairedPrinters by viewModel.pairedPrinters.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Toko & Struk, 1: Printer, 2: Staf & PIN, 3: Backup & Restore

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Pengaturan POLL POS",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs
        ScrollableTabRow(selectedTabIndex = activeTab, edgePadding = 0.dp) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Profil & Struk") })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1; viewModel.scanPairedPrinters() }, text = { Text("Printer Bluetooth") })
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Staf & Hak Akses") })
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }, text = { Text("Backup & Restore") })
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeTab) {
            0 -> StoreProfileTab(settings = settings, onSave = { viewModel.updateSettings(it) })
            1 -> PrinterTab(
                settings = settings,
                pairedPrinters = pairedPrinters,
                onScan = { viewModel.scanPairedPrinters() },
                onSelectPrinter = { addr, name ->
                    viewModel.updateSettings(settings.copy(bluetoothPrinterAddress = addr, bluetoothPrinterName = name))
                },
                onTestPrint = { addr -> viewModel.testPrint(addr) },
                onPaperSizeChange = { size -> viewModel.updateSettings(settings.copy(paperSize = size)) }
            )
            2 -> StaffManagementTab(
                allStaff = allStaff,
                currentStaff = currentStaff,
                onSaveStaff = { viewModel.saveStaff(it) },
                onDeleteStaff = { viewModel.deleteStaff(it) },
                onSwitchStaff = { viewModel.switchStaff(it) }
            )
            3 -> BackupRestoreTab(
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun StoreProfileTab(
    settings: StoreSettings,
    onSave: (StoreSettings) -> Unit
) {
    var storeName by remember(settings) { mutableStateOf(settings.storeName) }
    var address by remember(settings) { mutableStateOf(settings.address) }
    var phone by remember(settings) { mutableStateOf(settings.phone) }
    var logoUri by remember(settings) { mutableStateOf(settings.logoUri) }
    var taxRate by remember(settings) { mutableStateOf(settings.taxRate.toString()) }
    var enableTax by remember(settings) { mutableStateOf(settings.enableTax) }
    var serviceRate by remember(settings) { mutableStateOf(settings.serviceChargeRate.toString()) }
    var enableService by remember(settings) { mutableStateOf(settings.enableServiceCharge) }
    var showLogoOnReceipt by remember(settings) { mutableStateOf(settings.showLogoOnReceipt) }
    var customReceiptStoreName by remember(settings) { mutableStateOf(settings.customReceiptStoreName) }
    var receiptCustomLogoUri by remember(settings) { mutableStateOf(settings.receiptCustomLogoUri) }
    var receiptHeader by remember(settings) { mutableStateOf(settings.receiptHeader) }
    var receiptFooter by remember(settings) { mutableStateOf(settings.receiptFooter) }

    val storeLogoPicker = rememberProductImagePicker { path ->
        logoUri = path
    }
    val receiptLogoPicker = rememberProductImagePicker { path ->
        receiptCustomLogoUri = path
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Logo & Informasi Usaha
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Profil & Logo Toko", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                // Logo Picker Row
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            if (!logoUri.isNullOrBlank()) {
                                val logoData: Any = if (logoUri!!.startsWith("/")) File(logoUri!!) else logoUri!!
                                AsyncImage(
                                    model = logoData,
                                    contentDescription = "Logo Profil Toko",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Logo Profil Toko", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                "Digunakan pada menu navigasi dan profil toko Anda.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { storeLogoPicker.launch() },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (logoUri.isNullOrBlank()) "Pilih Logo" else "Ganti Logo", fontSize = 11.sp)
                                }
                                if (!logoUri.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = { logoUri = null },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text("Hapus", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Nama Toko / Resto / Cafe *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat Usaha") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Nomor Telepon") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Card 2: Pajak & Service Charge
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pajak & Service Charge", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Aktifkan Pajak (PPN)", fontWeight = FontWeight.SemiBold)
                        Text("Diterapkan pada setiap transaksi", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = enableTax, onCheckedChange = { enableTax = it })
                }

                if (enableTax) {
                    OutlinedTextField(
                        value = taxRate,
                        onValueChange = { taxRate = it },
                        label = { Text("Tarif Pajak (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Aktifkan Service Charge", fontWeight = FontWeight.SemiBold)
                        Text("Biaya layanan restoran / cafe", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = enableService, onCheckedChange = { enableService = it })
                }

                if (enableService) {
                    OutlinedTextField(
                        value = serviceRate,
                        onValueChange = { serviceRate = it },
                        label = { Text("Tarif Service Charge (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        // Card 3: Pengaturan & Tampilan Struk (Logo & Custom Toko)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tampilan & Kustomisasi Struk", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                // Custom Nama Toko di Struk
                OutlinedTextField(
                    value = customReceiptStoreName,
                    onValueChange = { customReceiptStoreName = it },
                    label = { Text("Nama Toko Khusus Struk (Opsional)") },
                    placeholder = { Text(storeName.ifBlank { "Sama dengan nama toko di profil" }) },
                    supportingText = { Text("Kosongkan jika ingin menggunakan nama toko utama di atas") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Toggle Tampilkan Logo di Struk
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tampilkan Logo di Struk", fontWeight = FontWeight.SemiBold)
                        Text("Cetak/tampilkan logo di bagian atas struk pembayaran", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = showLogoOnReceipt, onCheckedChange = { showLogoOnReceipt = it })
                }

                // If showLogoOnReceipt is enabled, show custom receipt logo selector
                if (showLogoOnReceipt) {
                    val activeReceiptLogo = receiptCustomLogoUri ?: logoUri
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(54.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                if (!activeReceiptLogo.isNullOrBlank()) {
                                    val logoData: Any = if (activeReceiptLogo.startsWith("/")) File(activeReceiptLogo) else activeReceiptLogo
                                    AsyncImage(
                                        model = logoData,
                                        contentDescription = "Logo Struk",
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    if (receiptCustomLogoUri != null) "Logo Khusus Struk Aktif" else if (logoUri != null) "Menggunakan Logo Profil Toko" else "Belum Ada Logo Struk",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { receiptLogoPicker.launch() },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pilih Logo Struk", fontSize = 10.sp)
                                    }
                                    if (receiptCustomLogoUri != null) {
                                        OutlinedButton(
                                            onClick = { receiptCustomLogoUri = null },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text("Pakai Logo Toko", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = receiptHeader,
                    onValueChange = { receiptHeader = it },
                    label = { Text("Header Struk (Atas)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = receiptFooter,
                    onValueChange = { receiptFooter = it },
                    label = { Text("Footer Struk (Bawah)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        }

        Button(
            onClick = {
                onSave(
                    settings.copy(
                        storeName = storeName.trim(),
                        address = address.trim(),
                        phone = phone.trim(),
                        logoUri = logoUri,
                        enableTax = enableTax,
                        taxRate = taxRate.toDoubleOrNull() ?: 10.0,
                        enableServiceCharge = enableService,
                        serviceChargeRate = serviceRate.toDoubleOrNull() ?: 5.0,
                        showLogoOnReceipt = showLogoOnReceipt,
                        customReceiptStoreName = customReceiptStoreName.trim(),
                        receiptCustomLogoUri = receiptCustomLogoUri,
                        receiptHeader = receiptHeader,
                        receiptFooter = receiptFooter
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simpan Pengaturan Toko", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun PrinterTab(
    settings: StoreSettings,
    pairedPrinters: List<BluetoothDevice>,
    onScan: () -> Unit,
    onSelectPrinter: (String, String) -> Unit,
    onTestPrint: (String) -> Unit,
    onPaperSizeChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Paper Size Selection
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ukuran Kertas Thermal Printer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = settings.paperSize == "58mm",
                        onClick = { onPaperSizeChange("58mm") },
                        label = { Text("58 mm (Standar Mobile)") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = settings.paperSize == "80mm",
                        onClick = { onPaperSizeChange("80mm") },
                        label = { Text("80 mm (Desktop / Kasir)") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Bluetooth Thermal Printer Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Printer Bluetooth Terhubung", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = if (settings.bluetoothPrinterName != null) "${settings.bluetoothPrinterName} (${settings.bluetoothPrinterAddress})" else "Belum ada printer dipilih",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(onClick = onScan, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan Ulang")
                    }
                }

                if (settings.bluetoothPrinterAddress != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onTestPrint(settings.bluetoothPrinterAddress) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak Halaman Uji (Test Print)")
                    }
                }
            }
        }

        // List of Paired Bluetooth Devices
        Text("Daftar Perangkat Bluetooth Tersedia:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        if (pairedPrinters.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tidak ada perangkat Bluetooth printer terpasang.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pastikan printer thermal Bluetooth Anda telah dipasangkan (paired) di pengaturan Bluetooth HP Android Anda, kemudian klik 'Scan Ulang'.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            pairedPrinters.forEach { dev ->
                val isSelected = settings.bluetoothPrinterAddress == dev.address
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(dev.name ?: "Unknown Device", fontWeight = FontWeight.Bold)
                            Text(dev.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!isSelected) {
                                Button(
                                    onClick = { onSelectPrinter(dev.address, dev.name ?: "Thermal Printer") },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Pilih", fontSize = 12.sp)
                                }
                            } else {
                                StatusBadge(text = "Aktif", color = AccentGreen)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StaffManagementTab(
    allStaff: List<StaffUser>,
    currentStaff: StaffUser?,
    onSaveStaff: (StaffUser) -> Unit,
    onDeleteStaff: (StaffUser) -> Unit,
    onSwitchStaff: (StaffUser) -> Unit
) {
    var isAddingStaff by remember { mutableStateOf(false) }
    var editingStaff by remember { mutableStateOf<StaffUser?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pengguna & Akses Staf", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Button(
                onClick = { isAddingStaff = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah Staf")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(allStaff) { staff ->
                val isCurrent = currentStaff?.id == staff.id
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                    ),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (isCurrent) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    StatusBadge(text = "Sedang Login", color = AccentGreen)
                                }
                            }
                            Text("Role: ${staff.role} • PIN: ****", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Izin: " + listOfNotNull(
                                    if (staff.canDiscount) "Diskon" else null,
                                    if (staff.canRefund) "Refund" else null,
                                    if (staff.canVoid) "Void" else null,
                                    if (staff.canViewReports) "Laporan" else null
                                ).joinToString(", "),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Row {
                            if (!isCurrent) {
                                TextButton(onClick = { onSwitchStaff(staff) }) {
                                    Text("Pilih Kasir", fontSize = 12.sp)
                                }
                            }
                            IconButton(onClick = { editingStaff = staff }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                            if (staff.role != "OWNER") {
                                IconButton(onClick = { onDeleteStaff(staff) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isAddingStaff || editingStaff != null) {
        val target = editingStaff
        var name by remember { mutableStateOf(target?.name ?: "") }
        var role by remember { mutableStateOf(target?.role ?: "KASIR") }
        var pin by remember { mutableStateOf(target?.pin ?: "") }
        var canDiscount by remember { mutableStateOf(target?.canDiscount ?: true) }
        var canRefund by remember { mutableStateOf(target?.canRefund ?: false) }
        var canVoid by remember { mutableStateOf(target?.canVoid ?: false) }
        var canReports by remember { mutableStateOf(target?.canViewReports ?: false) }

        Dialog(onDismissRequest = { isAddingStaff = false; editingStaff = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(0.92f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (target == null) "Tambah Staf Baru" else "Edit Data Staf",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Staf *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Role Chip Selector
                    Text("Peran / Role:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("OWNER", "ADMIN", "KASIR", "KITCHEN").forEach { r ->
                            FilterChip(
                                selected = role == r,
                                onClick = { role = r },
                                label = { Text(r, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { ch -> ch.isDigit() } },
                        label = { Text("PIN Keamanan (4 Digit Angka) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Hak Akses:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canDiscount, onCheckedChange = { canDiscount = it })
                        Text("Beri Diskon", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canRefund, onCheckedChange = { canRefund = it })
                        Text("Lakukan Refund", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canVoid, onCheckedChange = { canVoid = it })
                        Text("Lakukan Void", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = canReports, onCheckedChange = { canReports = it })
                        Text("Akses Laporan", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { isAddingStaff = false; editingStaff = null }) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank() && pin.isNotBlank()) {
                                    val user = StaffUser(
                                        id = target?.id ?: 0L,
                                        name = name.trim(),
                                        role = role,
                                        pin = pin.trim(),
                                        canDiscount = canDiscount,
                                        canRefund = canRefund,
                                        canVoid = canVoid,
                                        canViewReports = canReports
                                    )
                                    onSaveStaff(user)
                                    isAddingStaff = false
                                    editingStaff = null
                                }
                            }
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackupRestoreTab(
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    var restoreJsonInput by remember { mutableStateOf("") }
    var showRestoreDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Backup card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Cadangkan Database (Backup Lokal)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Buat cadangan lengkap seluruh data produk, transaksi, stok, pelanggan, dan pengaturan toko ke file JSON yang dapat disimpan di memori internal atau dibagikan.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { viewModel.backupDatabaseToFile(context) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buat & Unduh File Backup")
                }
            }
        }

        // Restore card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = AccentGreen)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Pulihkan Data (Restore File)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Kembalikan data dari file cadangan sebelumnya. Tempel teks backup atau pilih file cadangan.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { showRestoreDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka Form Restore Data")
                }
            }
        }

        // Google Drive Info Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Color(0xFF3F51B5))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Google Drive & Cloud (Opsional)", fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "POLL POS dirancang 100% offline-first. Data utama selalu aman di memori perangkat HP/tablet Anda tanpa memerlukan kuota atau sinyal. File hasil 'Cadangkan Database' dapat Anda simpan ke Google Drive kapan saja saat Anda memiliki koneksi internet sebagai salinan tambahan.",
                    fontSize = 12.sp,
                    color = Color(0xFF283593),
                    lineHeight = 18.sp
                )
            }
        }
    }

    if (showRestoreDialog) {
        Dialog(onDismissRequest = { showRestoreDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.7f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Restore Database Dari JSON", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Tempel teks isi file JSON backup di bawah ini:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        placeholder = { Text("{\"version\": 1, \"products\": [...]} ") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showRestoreDialog = false }) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (restoreJsonInput.isNotBlank()) {
                                    viewModel.restoreDatabaseFromJson(restoreJsonInput)
                                    showRestoreDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Mulai Restore")
                        }
                    }
                }
            }
        }
    }
}
