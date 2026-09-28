package com.example.ui.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.export.DataExportHelper
import com.example.printer.EscPosPrinterHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class NavScreen(val title: String, val icon: String) {
    DASHBOARD("Dashboard", "dashboard"),
    CASHIER("Kasir", "point_of_sale"),
    PRODUCTS("Produk", "inventory_2"),
    CATEGORIES("Kategori", "category"),
    STOCK("Stok", "warehouse"),
    CUSTOMERS("Pelanggan", "people"),
    TRANSACTIONS("Transaksi", "receipt_long"),
    REPORTS("Laporan", "analytics"),
    KITCHEN("Dapur", "soup_kitchen"),
    CASH("Kas", "account_balance_wallet"),
    SETTINGS("Pengaturan", "settings")
}

data class PaymentSplitEntry(
    val method: String,
    val amount: Double
)

enum class ReportPeriod {
    TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, ALL_TIME
}

/**
 * PosViewModel
 *
 * Inherits all Cart and Inventory state management from [BaseCartInventoryViewModel].
 * Adds POS-specific features: Navigation, Staff Authentication & PIN, Split Payments,
 * Checkout & Order Processing, Cash Shifts, Bluetooth Receipt Printing, Reports, and Data Backup/Restore.
 */
class PosViewModel(application: Application) : BaseCartInventoryViewModel(application) {

    // Current navigation screen
    private val _currentScreen = MutableStateFlow(NavScreen.DASHBOARD)
    val currentScreen: StateFlow<NavScreen> = _currentScreen.asStateFlow()

    // Screen backstack history for BackHandler
    private val screenHistory = mutableListOf(NavScreen.DASHBOARD)

    // Current Staff and Authentication
    val allStaff: StateFlow<List<StaffUser>> = repository.allStaff
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentStaff = MutableStateFlow<StaffUser?>(null)
    val currentStaff: StateFlow<StaffUser?> = _currentStaff.asStateFlow()

    private val _isPinLocked = MutableStateFlow(false)
    val isPinLocked: StateFlow<Boolean> = _isPinLocked.asStateFlow()

    // Store Settings
    val storeSettings: StateFlow<StoreSettings> = repository.storeSettings
        .map { it ?: StoreSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoreSettings())

    // Transactions, Customers, Kitchen & Shifts
    val allCustomers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeKitchenOrders: StateFlow<List<Transaction>> = repository.activeKitchenOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentOpenShift: StateFlow<CashShift?> = repository.currentOpenShift
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allShifts: StateFlow<List<CashShift>> = repository.allShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Split Payments
    private val _splitPayments = MutableStateFlow<List<PaymentSplitEntry>>(emptyList())
    val splitPayments: StateFlow<List<PaymentSplitEntry>> = _splitPayments.asStateFlow()

    // Last completed transaction for receipt preview
    private val _lastCompletedTransaction = MutableStateFlow<Triple<Transaction, List<TransactionItem>, List<TransactionPayment>>?>(null)
    val lastCompletedTransaction: StateFlow<Triple<Transaction, List<TransactionItem>, List<TransactionPayment>>?> = _lastCompletedTransaction.asStateFlow()

    // Bluetooth printers list
    private val _pairedPrinters = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedPrinters: StateFlow<List<BluetoothDevice>> = _pairedPrinters.asStateFlow()

    // Report filter period
    private val _reportPeriod = MutableStateFlow(ReportPeriod.TODAY)
    val reportPeriod: StateFlow<ReportPeriod> = _reportPeriod.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initDefaultDataIfEmpty()
            // Set initial staff if none set
            val staffList = repository.allStaff.firstOrNull() ?: emptyList()
            if (staffList.isNotEmpty()) {
                _currentStaff.value = staffList.firstOrNull { it.role == "OWNER" } ?: staffList.first()
            }
        }
    }

    override fun clearCart() {
        super.clearCart()
        _splitPayments.value = emptyList()
    }

    override fun adjustStock(productId: Long, productName: String, type: String, qty: Double, note: String) {
        val staffName = _currentStaff.value?.name ?: "Admin"
        super.adjustStock(productId, productName, type, qty, staffName, note)
    }

    fun navigateTo(screen: NavScreen) {
        if (_currentScreen.value != screen) {
            screenHistory.add(screen)
            _currentScreen.value = screen
        }
    }

    fun handleBack(): Boolean {
        if (screenHistory.size > 1) {
            screenHistory.removeAt(screenHistory.lastIndex)
            _currentScreen.value = screenHistory.last()
            return true
        }
        return false
    }

    // --- Authentication & Staff ---
    fun loginWithPin(pin: String): Boolean {
        val staffList = allStaff.value
        val matched = staffList.firstOrNull { it.pin == pin && it.isActive }
        if (matched != null) {
            _currentStaff.value = matched
            _isPinLocked.value = false
            showSnackbar("Selamat datang, ${matched.name} (${matched.role})")
            return true
        }
        showSnackbar("PIN tidak sesuai")
        return false
    }

    fun lockApp() {
        _isPinLocked.value = true
    }

    fun unlockApp() {
        _isPinLocked.value = false
    }

    fun switchStaff(staff: StaffUser) {
        _currentStaff.value = staff
        showSnackbar("Beralih kasir ke ${staff.name}")
    }

    fun saveStaff(staff: StaffUser) {
        viewModelScope.launch {
            repository.saveStaff(staff)
            showSnackbar("Data staf ${staff.name} berhasil disimpan")
        }
    }

    fun deleteStaff(staff: StaffUser) {
        viewModelScope.launch {
            repository.deleteStaff(staff)
            showSnackbar("Staf dihapus")
        }
    }

    // Payments handling
    fun addSplitPayment(method: String, amount: Double) {
        val current = _splitPayments.value.toMutableList()
        current.add(PaymentSplitEntry(method, amount))
        _splitPayments.value = current
    }

    fun removeSplitPayment(entry: PaymentSplitEntry) {
        val current = _splitPayments.value.toMutableList()
        current.remove(entry)
        _splitPayments.value = current
    }

    fun clearSplitPayments() {
        _splitPayments.value = emptyList()
    }

    // Complete Checkout
    fun processCheckout(
        singlePaymentMethod: String? = null,
        tenderedCash: Double = 0.0,
        onSuccess: (Transaction) -> Unit
    ) {
        val items = _cart.value
        if (items.isEmpty()) {
            showSnackbar("Keranjang belanja masih kosong")
            return
        }

        val total = cartGrandTotal.value
        val subtotal = cartSubtotal.value
        val discount = cartDiscountAmount.value
        val tax = cartTaxAmount.value
        val taxRate = if (storeSettings.value.enableTax) storeSettings.value.taxRate else 0.0
        val serviceCharge = cartServiceChargeAmount.value

        val paymentList = mutableListOf<TransactionPayment>()
        var changeAmount = 0.0

        if (singlePaymentMethod != null) {
            // Single payment method
            val paidAmount = if (singlePaymentMethod.equals("Tunai", ignoreCase = true) && tenderedCash >= total) tenderedCash else total
            if (singlePaymentMethod.equals("Tunai", ignoreCase = true)) {
                changeAmount = (paidAmount - total).coerceAtLeast(0.0)
            }
            paymentList.add(TransactionPayment(paymentMethod = singlePaymentMethod, amount = paidAmount))
        } else {
            // Split payment
            val splits = _splitPayments.value
            val totalPaid = splits.sumOf { it.amount }
            if (totalPaid < total) {
                showSnackbar("Pembayaran kurang Rp${(total - totalPaid).toLong()}")
                return
            }
            splits.forEach {
                paymentList.add(TransactionPayment(paymentMethod = it.method, amount = it.amount))
            }
            changeAmount = (totalPaid - total).coerceAtLeast(0.0)
        }

        val transactionItems = items.map {
            TransactionItem(
                productId = it.product.id,
                productName = it.product.name,
                price = it.unitPrice,
                costPrice = it.product.costPrice,
                quantity = it.quantity,
                unit = it.product.unit,
                variantNote = it.variantSummary + if (it.notes.isNotBlank()) " (${it.notes})" else "",
                total = it.totalPrice
            )
        }

        val cashier = _currentStaff.value?.name ?: "Kasir"

        viewModelScope.launch {
            val result = repository.completeTransaction(
                customer = _selectedCustomer.value,
                cashierName = cashier,
                items = transactionItems,
                payments = paymentList,
                subtotal = subtotal,
                discountAmount = discount,
                discountNote = _discountNote.value,
                taxAmount = tax,
                taxRate = taxRate,
                serviceCharge = serviceCharge,
                total = total,
                changeAmount = changeAmount,
                tableNo = _orderTableNo.value,
                notes = _orderNotes.value
            )

            _lastCompletedTransaction.value = result
            clearCart()
            showSnackbar("Transaksi #${result.first.transactionNo} berhasil diselesaikan!")
            onSuccess(result.first)
        }
    }

    fun dismissReceipt() {
        _lastCompletedTransaction.value = null
    }

    // --- Customer Management ---
    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            showSnackbar("Pelanggan ${customer.name} berhasil disimpan")
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            showSnackbar("Pelanggan ${customer.name} berhasil dihapus")
        }
    }

    // --- Transaction Actions: Refund / Void / Reprint ---
    fun refundTransaction(transaction: Transaction, reason: String) {
        val staff = _currentStaff.value?.name ?: "Admin"
        viewModelScope.launch {
            repository.refundTransaction(transaction, reason, staff)
            showSnackbar("Transaksi #${transaction.transactionNo} berhasil di-refund")
        }
    }

    fun voidTransaction(transaction: Transaction, reason: String) {
        val staff = _currentStaff.value?.name ?: "Admin"
        viewModelScope.launch {
            repository.voidTransaction(transaction, reason, staff)
            showSnackbar("Transaksi #${transaction.transactionNo} telah di-void")
        }
    }

    fun loadTransactionForReceipt(transaction: Transaction) {
        viewModelScope.launch {
            val (items, payments) = repository.getTransactionDetails(transaction.id)
            _lastCompletedTransaction.value = Triple(transaction, items, payments)
        }
    }

    // --- Kitchen KDS ---
    fun updateKitchenStatus(transactionId: Long, status: String) {
        viewModelScope.launch {
            repository.updateKitchenStatus(transactionId, status)
            showSnackbar("Status pesanan diubah ke $status")
        }
    }

    // --- Cash Management ---
    fun openShift(initialCash: Double) {
        val staff = _currentStaff.value?.name ?: "Kasir"
        viewModelScope.launch {
            repository.openCashShift(staff, initialCash)
            showSnackbar("Shift kas dibuka dengan modal Rp${initialCash.toLong()}")
        }
    }

    fun addCashEntry(type: String, amount: Double, note: String, category: String = "Operasional") {
        val shift = currentOpenShift.value ?: return
        val staff = _currentStaff.value?.name ?: "Kasir"
        viewModelScope.launch {
            repository.addCashEntry(shift.id, type, amount, note, staff, category)
            showSnackbar(if (type == "PAY_IN") "Kas Masuk berhasil dicatat" else "Kas Keluar berhasil dicatat")
        }
    }

    fun closeShift(actualCash: Double, notes: String) {
        val shift = currentOpenShift.value ?: return
        viewModelScope.launch {
            repository.closeCashShift(shift.id, actualCash, notes)
            showSnackbar("Shift kas berhasil ditutup")
        }
    }

    fun getShiftEntries(shiftId: Long): Flow<List<CashEntry>> = repository.getEntriesForShift(shiftId)

    // --- Settings & Bluetooth Printer ---
    fun updateSettings(settings: StoreSettings) {
        viewModelScope.launch {
            repository.updateStoreSettings(settings)
            showSnackbar("Pengaturan toko berhasil disimpan")
        }
    }

    fun scanPairedPrinters() {
        _pairedPrinters.value = EscPosPrinterHelper.getPairedBluetoothDevices()
    }

    fun printCurrentReceipt(deviceAddress: String?) {
        val currentTx = _lastCompletedTransaction.value ?: return
        val settings = storeSettings.value
        val addr = deviceAddress ?: settings.bluetoothPrinterAddress

        if (addr.isNullOrBlank()) {
            showSnackbar("Printer Bluetooth belum dipilih di Pengaturan")
            return
        }

        val bytes = EscPosPrinterHelper.buildEscPosBytes(settings, currentTx.first, currentTx.second, currentTx.third)
        viewModelScope.launch {
            val result = EscPosPrinterHelper.printToBluetoothDevice(addr, bytes)
            result.onSuccess { showSnackbar(it) }
            result.onFailure { showSnackbar(it.message ?: "Gagal mencetak struk") }
        }
    }

    fun testPrint(deviceAddress: String) {
        val settings = storeSettings.value
        val bytes = EscPosPrinterHelper.buildTestPrintBytes(settings)
        viewModelScope.launch {
            val result = EscPosPrinterHelper.printToBluetoothDevice(deviceAddress, bytes)
            result.onSuccess { showSnackbar("Test print berhasil!") }
            result.onFailure { showSnackbar(it.message ?: "Gagal test print") }
        }
    }

    // --- Reports Filtering ---
    fun setReportPeriod(period: ReportPeriod) {
        _reportPeriod.value = period
    }

    fun getFilteredTransactions(period: ReportPeriod, allList: List<Transaction>): List<Transaction> {
        val cal = getStartOfDayCalendar()
        val startOfDay = cal.timeInMillis

        return when (period) {
            ReportPeriod.TODAY -> allList.filter { it.timestamp >= startOfDay }
            ReportPeriod.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val startOfYesterday = cal.timeInMillis
                allList.filter { it.timestamp >= startOfYesterday && it.timestamp < startOfDay }
            }
            ReportPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val startOfWeek = cal.timeInMillis
                allList.filter { it.timestamp >= startOfWeek }
            }
            ReportPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val startOfMonth = cal.timeInMillis
                allList.filter { it.timestamp >= startOfMonth }
            }
            ReportPeriod.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                val startOfYear = cal.timeInMillis
                allList.filter { it.timestamp >= startOfYear }
            }
            ReportPeriod.ALL_TIME -> allList
        }
    }

    // --- Export & Backup ---
    fun exportTransactionsCsv(context: Context, transactions: List<Transaction>) {
        viewModelScope.launch {
            try {
                val file = DataExportHelper.exportTransactionsToCsv(context, transactions)
                DataExportHelper.shareFile(context, file)
                showSnackbar("Laporan transaksi CSV berhasil diexport")
            } catch (e: Exception) {
                showSnackbar("Gagal export: ${e.message}")
            }
        }
    }

    fun exportProductsCsv(context: Context) {
        viewModelScope.launch {
            try {
                val cats = allCategories.value.associate { it.id to it.name }
                val file = DataExportHelper.exportProductsToCsv(context, allProducts.value, cats)
                DataExportHelper.shareFile(context, file)
                showSnackbar("Data produk CSV berhasil diexport")
            } catch (e: Exception) {
                showSnackbar("Gagal export produk: ${e.message}")
            }
        }
    }

    fun exportCustomersCsv(context: Context) {
        viewModelScope.launch {
            try {
                val file = DataExportHelper.exportCustomersToCsv(context, allCustomers.value)
                DataExportHelper.shareFile(context, file)
                showSnackbar("Data pelanggan CSV berhasil diexport")
            } catch (e: Exception) {
                showSnackbar("Gagal export pelanggan: ${e.message}")
            }
        }
    }

    fun exportCashShiftsCsv(context: Context) {
        viewModelScope.launch {
            try {
                val file = DataExportHelper.exportCashShiftsToCsv(context, allShifts.value)
                DataExportHelper.shareFile(context, file)
                showSnackbar("Laporan kas CSV berhasil diexport")
            } catch (e: Exception) {
                showSnackbar("Gagal export kas: ${e.message}")
            }
        }
    }

    fun backupDatabaseToFile(context: Context) {
        viewModelScope.launch {
            try {
                val json = repository.exportDatabaseToJson()
                val fileName = "POLL_POS_BACKUP_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                val dir = File(context.cacheDir, "exports")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                file.writeText(json)
                DataExportHelper.shareFile(context, file, "application/json")
                showSnackbar("Backup database POLL POS berhasil dibuat ($fileName)")
            } catch (e: Exception) {
                showSnackbar("Gagal membuat backup: ${e.message}")
            }
        }
    }

    fun restoreDatabaseFromJson(jsonString: String) {
        viewModelScope.launch {
            val success = repository.restoreDatabaseFromJson(jsonString)
            if (success) {
                showSnackbar("Database berhasil di-restore!")
            } else {
                showSnackbar("Gagal restore: format file backup tidak valid")
            }
        }
    }
}

private fun getStartOfDayCalendar(): Calendar {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal
}
