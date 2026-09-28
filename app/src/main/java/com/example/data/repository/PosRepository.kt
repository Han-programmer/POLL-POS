package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class PosRepository(private val database: AppDatabase) {
    private val productDao = database.productDao()
    private val categoryDao = database.categoryDao()
    private val customerDao = database.customerDao()
    private val transactionDao = database.transactionDao()
    private val stockDao = database.stockDao()
    private val cashShiftDao = database.cashShiftDao()
    private val staffDao = database.staffDao()
    private val settingsDao = database.storeSettingsDao()

    // Reactive Flows
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val activeProducts: Flow<List<Product>> = productDao.getActiveProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val activeKitchenOrders: Flow<List<Transaction>> = transactionDao.getActiveKitchenOrders()
    val allStockMovements: Flow<List<StockMovement>> = stockDao.getAllStockMovements()
    val currentOpenShift: Flow<CashShift?> = cashShiftDao.getCurrentOpenShift()
    val allShifts: Flow<List<CashShift>> = cashShiftDao.getAllShifts()
    val allStaff: Flow<List<StaffUser>> = staffDao.getAllStaff()
    val storeSettings: Flow<StoreSettings?> = settingsDao.getSettings()

    suspend fun initDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        if (categoryDao.countCategories() == 0) {
            val categories = listOf(
                Category(name = "Kopi & Minuman", iconName = "local_cafe", colorHex = "#D32F2F", sortOrder = 1),
                Category(name = "Makanan Utama", iconName = "restaurant", colorHex = "#C62828", sortOrder = 2),
                Category(name = "Snack & Camilan", iconName = "bakery_dining", colorHex = "#B71C1C", sortOrder = 3),
                Category(name = "Paket Hemat", iconName = "sell", colorHex = "#E53935", sortOrder = 4),
                Category(name = "Retail & UMKM", iconName = "storefront", colorHex = "#8E0000", sortOrder = 5)
            )
            categoryDao.insertAll(categories)
        }

        if (productDao.countProducts() == 0) {
            val cats = categoryDao.getAllCategories().firstOrNull() ?: emptyList()
            val catDrink = cats.getOrNull(0)?.id ?: 1L
            val catFood = cats.getOrNull(1)?.id ?: 2L
            val catSnack = cats.getOrNull(2)?.id ?: 3L
            val catRetail = cats.getOrNull(4)?.id ?: 5L

            val products = listOf(
                Product(
                    name = "Kopi Susu Gula Aren",
                    price = 18000.0,
                    costPrice = 8000.0,
                    stock = 50.0,
                    minStock = 10.0,
                    sku = "DRK-001",
                    barcode = "8991001",
                    categoryId = catDrink,
                    unit = "cup",
                    description = "Espresso house blend dengan susu segar dan gula aren asli",
                    imageUri = "res:img_kopi_susu",
                    variantsJson = "[{\"name\":\"Suhu\",\"options\":[{\"name\":\"Dingin\",\"extraPrice\":0},{\"name\":\"Panas\",\"extraPrice\":0}]},{\"name\":\"Gula\",\"options\":[{\"name\":\"Normal\",\"extraPrice\":0},{\"name\":\"Less Sugar\",\"extraPrice\":0}]}]"
                ),
                Product(
                    name = "Americano / Long Black",
                    price = 15000.0,
                    costPrice = 5000.0,
                    stock = 60.0,
                    minStock = 10.0,
                    sku = "DRK-002",
                    barcode = "8991002",
                    categoryId = catDrink,
                    unit = "cup",
                    imageUri = "res:img_kopi_susu",
                    description = "Double shot espresso dengan air murni"
                ),
                Product(
                    name = "Es Matcha Latte",
                    price = 22000.0,
                    costPrice = 10000.0,
                    stock = 35.0,
                    minStock = 5.0,
                    sku = "DRK-003",
                    barcode = "8991003",
                    categoryId = catDrink,
                    unit = "cup",
                    imageUri = "res:img_es_teh",
                    description = "Matcha Jepang murni dipadu susu segar creamy"
                ),
                Product(
                    name = "Es Teh Manis Jumbo",
                    price = 6000.0,
                    costPrice = 2000.0,
                    stock = 120.0,
                    minStock = 20.0,
                    sku = "DRK-004",
                    barcode = "8991004",
                    categoryId = catDrink,
                    unit = "cup",
                    imageUri = "res:img_es_teh",
                    description = "Teh melati wangi khas Nusantara gelas jumbo 22oz"
                ),
                Product(
                    name = "Nasi Goreng Spesial POLL",
                    price = 25000.0,
                    costPrice = 12000.0,
                    stock = 40.0,
                    minStock = 8.0,
                    sku = "FOD-001",
                    barcode = "8992001",
                    categoryId = catFood,
                    unit = "porsi",
                    imageUri = "res:img_nasi_goreng",
                    description = "Nasi goreng bumbu racikan rahasia dengan suwiran ayam, sosis, dan telur",
                    variantsJson = "[{\"name\":\"Level Pedas\",\"options\":[{\"name\":\"Tidak Pedas\",\"extraPrice\":0},{\"name\":\"Sedang\",\"extraPrice\":0},{\"name\":\"Pedas\",\"extraPrice\":0}]},{\"name\":\"Telur\",\"options\":[{\"name\":\"Ceplok\",\"extraPrice\":0},{\"name\":\"Dadar\",\"extraPrice\":0}]}]"
                ),
                Product(
                    name = "Ayam Bakar Madu + Nasi",
                    price = 28000.0,
                    costPrice = 14000.0,
                    stock = 30.0,
                    minStock = 5.0,
                    sku = "FOD-002",
                    barcode = "8992002",
                    categoryId = catFood,
                    unit = "porsi",
                    imageUri = "res:img_nasi_goreng",
                    description = "Paha ayam bakar bumbu madu legit disajikan dengan nasi hangat dan lalapan"
                ),
                Product(
                    name = "Mie Goreng Jawa",
                    price = 22000.0,
                    costPrice = 10000.0,
                    stock = 35.0,
                    minStock = 5.0,
                    sku = "FOD-003",
                    barcode = "8992003",
                    categoryId = catFood,
                    unit = "porsi",
                    imageUri = "res:img_nasi_goreng",
                    description = "Mie kuning kenyal dengan bumbu ebi, sayuran segar, dan irisan bakso"
                ),
                Product(
                    name = "Kentang Goreng Crispy",
                    price = 15000.0,
                    costPrice = 7000.0,
                    stock = 40.0,
                    minStock = 8.0,
                    sku = "SNK-001",
                    barcode = "8993001",
                    categoryId = catSnack,
                    unit = "porsi",
                    imageUri = "res:img_croissant",
                    description = "French fries renyah dengan taburan bumbu keju / BBQ"
                ),
                Product(
                    name = "Butter Croissant Perancis",
                    price = 18000.0,
                    costPrice = 9000.0,
                    stock = 25.0,
                    minStock = 5.0,
                    sku = "SNK-003",
                    barcode = "8993003",
                    categoryId = catSnack,
                    unit = "pcs",
                    imageUri = "res:img_croissant",
                    description = "Croissant renyah berlapis dengan butter impor hangat"
                ),
                Product(
                    name = "Roti Bakar Coklat Keju",
                    price = 16000.0,
                    costPrice = 8000.0,
                    stock = 30.0,
                    minStock = 5.0,
                    sku = "SNK-002",
                    barcode = "8993002",
                    categoryId = catSnack,
                    unit = "porsi",
                    imageUri = "res:img_croissant",
                    description = "Roti tawar tebal dipanggang dengan filling meses coklat dan parutan keju gurih"
                ),
                Product(
                    name = "Keripik Tempe Renyah UMKM",
                    price = 12000.0,
                    costPrice = 8000.0,
                    stock = 25.0,
                    minStock = 5.0,
                    sku = "RTL-001",
                    barcode = "8994001",
                    categoryId = catRetail,
                    unit = "bungkus",
                    description = "Produk UMKM lokal, keripik tempe gurih renyah tanpa pengawet 150g"
                )
            )
            productDao.insertAll(products)
        } else {
            // Backfill imageUri for existing products if blank
            val existingProducts = productDao.getAllProductsSync()
            existingProducts.forEach { p ->
                if (p.imageUri.isNullOrBlank()) {
                    val lower = p.name.lowercase()
                    val mappedUri = when {
                        lower.contains("kopi") || lower.contains("coffee") || lower.contains("espresso") || lower.contains("americano") -> "res:img_kopi_susu"
                        lower.contains("nasi") || lower.contains("ayam") || lower.contains("mie") -> "res:img_nasi_goreng"
                        lower.contains("croissant") || lower.contains("roti") || lower.contains("kentang") -> "res:img_croissant"
                        lower.contains("teh") || lower.contains("tea") || lower.contains("matcha") -> "res:img_es_teh"
                        else -> null
                    }
                    if (mappedUri != null) {
                        productDao.updateProduct(p.copy(imageUri = mappedUri))
                    }
                }
            }
        }

        if (customerDao.countCustomers() == 0) {
            val customers = listOf(
                Customer(name = "Budi Santoso", phone = "081234567890", address = "Jl. Sudirman No. 12, Jakarta", notes = "Langganan kopi pagi", discountPercent = 5.0),
                Customer(name = "Siti Nurhaliza", phone = "085712345678", address = "Jl. Merdeka No. 8, Bandung", notes = "Suka meja no 4", discountPercent = 0.0),
                Customer(name = "Dimas Pratama", phone = "087890123456", address = "Jl. Gatot Subroto No. 20, Surabaya", notes = "Member VIP", discountPercent = 10.0)
            )
            customerDao.insertAll(customers)
        }

        if (staffDao.countStaff() == 0) {
            val staff = listOf(
                StaffUser(name = "Owner", role = "OWNER", pin = "1234"),
                StaffUser(name = "Kasir 1", role = "KASIR", pin = "1111", canRefund = false, canVoid = false, canChangePrice = false, canManageSettings = false),
                StaffUser(name = "Chef Dapur", role = "KITCHEN", pin = "2222", canDiscount = false, canRefund = false, canVoid = false, canChangePrice = false, canManageStock = false, canViewReports = false, canManageSettings = false)
            )
            staffDao.insertAll(staff)
        }

        if (settingsDao.getSettingsDirect() == null) {
            settingsDao.saveSettings(StoreSettings())
        }
    }

    // Product & Category operations
    suspend fun saveProduct(product: Product): Long = withContext(Dispatchers.IO) {
        if (product.id == 0L) productDao.insertProduct(product)
        else {
            productDao.updateProduct(product)
            product.id
        }
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    suspend fun saveCategory(category: Category): Long = withContext(Dispatchers.IO) {
        if (category.id == 0L) categoryDao.insertCategory(category)
        else {
            categoryDao.updateCategory(category)
            category.id
        }
    }

    suspend fun deleteCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategory(category)
    }

    // Customer operations
    suspend fun saveCustomer(customer: Customer): Long = withContext(Dispatchers.IO) {
        if (customer.id == 0L) customerDao.insertCustomer(customer)
        else {
            customerDao.updateCustomer(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomer(customer)
    }

    // Stock operations
    suspend fun adjustStock(
        productId: Long,
        productName: String,
        movementType: String,
        quantity: Double,
        staffName: String = "Admin",
        note: String = ""
    ) = withContext(Dispatchers.IO) {
        val product = productDao.getProductById(productId) ?: return@withContext
        val oldStock = product.stock
        val newStock = when (movementType) {
            "IN" -> oldStock + quantity
            "OUT" -> (oldStock - quantity).coerceAtLeast(0.0)
            "OPNAME", "ADJUSTMENT" -> quantity
            else -> oldStock
        }
        productDao.updateStock(productId, newStock)
        stockDao.insertMovement(
            StockMovement(
                productId = productId,
                productName = productName,
                movementType = movementType,
                quantity = quantity,
                previousStock = oldStock,
                newStock = newStock,
                note = note,
                staffName = staffName
            )
        )
    }

    // Transaction Checkout (Atomic execution)
    suspend fun completeTransaction(
        customer: Customer?,
        cashierName: String,
        items: List<TransactionItem>,
        payments: List<TransactionPayment>,
        subtotal: Double,
        discountAmount: Double,
        discountNote: String,
        taxAmount: Double,
        taxRate: Double,
        serviceCharge: Double,
        total: Double,
        changeAmount: Double,
        tableNo: String,
        notes: String
    ): Triple<Transaction, List<TransactionItem>, List<TransactionPayment>> = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
        val randomSuffix = Random.nextInt(100, 999)
        val transactionNo = "TRX-${dateFormat.format(Date())}-$randomSuffix"

        val paymentSummary = payments.joinToString(", ") {
            val amountFormatted = String.format(Locale.GERMANY, "%,.0f", it.amount)
            "${it.paymentMethod}: Rp$amountFormatted"
        }

        val transaction = Transaction(
            transactionNo = transactionNo,
            timestamp = System.currentTimeMillis(),
            customerId = customer?.id,
            customerName = customer?.name ?: "Pelanggan Umum",
            customerPhone = customer?.phone ?: "",
            cashierName = cashierName,
            subtotal = subtotal,
            discountAmount = discountAmount,
            discountNote = discountNote,
            taxAmount = taxAmount,
            taxRate = taxRate,
            serviceCharge = serviceCharge,
            total = total,
            paymentStatus = "PAID",
            paymentMethodsSummary = paymentSummary,
            changeAmount = changeAmount,
            notes = notes,
            tableNo = tableNo,
            kitchenStatus = "BARU"
        )

        val transactionId = transactionDao.insertTransaction(transaction)

        val itemsWithTxId = items.map { it.copy(transactionId = transactionId) }
        transactionDao.insertTransactionItems(itemsWithTxId)

        val paymentsWithTxId = payments.map { it.copy(transactionId = transactionId) }
        transactionDao.insertTransactionPayments(paymentsWithTxId)

        // Deduct inventory & record movement
        for (item in items) {
            val p = productDao.getProductById(item.productId)
            if (p != null) {
                val oldStock = p.stock
                val newStock = (oldStock - item.quantity).coerceAtLeast(0.0)
                productDao.updateStock(p.id, newStock)
                stockDao.insertMovement(
                    StockMovement(
                        productId = p.id,
                        productName = p.name,
                        movementType = "SALE",
                        quantity = item.quantity,
                        previousStock = oldStock,
                        newStock = newStock,
                        note = "Transaksi #$transactionNo",
                        staffName = cashierName
                    )
                )
            }
        }

        // Update customer total spent
        if (customer != null) {
            customerDao.updateCustomerStats(customer.id, total)
        }

        // Update Cash shift if cash payment used
        val cashPaid = payments.filter { it.paymentMethod.equals("Tunai", ignoreCase = true) }.sumOf { it.amount } - changeAmount
        if (cashPaid > 0) {
            val openShift = cashShiftDao.getCurrentOpenShiftDirect()
            if (openShift != null) {
                val updatedShift = openShift.copy(
                    totalCashSales = openShift.totalCashSales + cashPaid,
                    expectedCash = openShift.initialCash + openShift.totalPayIn - openShift.totalPayOut + (openShift.totalCashSales + cashPaid)
                )
                cashShiftDao.updateShift(updatedShift)
            }
        }

        val completedTx = transaction.copy(id = transactionId)
        Triple(completedTx, itemsWithTxId, paymentsWithTxId)
    }

    suspend fun getTransactionDetails(transactionId: Long): Pair<List<TransactionItem>, List<TransactionPayment>> = withContext(Dispatchers.IO) {
        val items = transactionDao.getItemsForTransaction(transactionId)
        val payments = transactionDao.getPaymentsForTransaction(transactionId)
        Pair(items, payments)
    }

    suspend fun refundTransaction(transaction: Transaction, reason: String, staffName: String) = withContext(Dispatchers.IO) {
        transactionDao.updateTransactionStatus(transaction.id, "REFUNDED")
        val items = transactionDao.getItemsForTransaction(transaction.id)
        for (item in items) {
            val p = productDao.getProductById(item.productId)
            if (p != null) {
                val oldStock = p.stock
                val newStock = oldStock + item.quantity
                productDao.updateStock(p.id, newStock)
                stockDao.insertMovement(
                    StockMovement(
                        productId = p.id,
                        productName = p.name,
                        movementType = "REFUND",
                        quantity = item.quantity,
                        previousStock = oldStock,
                        newStock = newStock,
                        note = "Refund #${transaction.transactionNo}: $reason",
                        staffName = staffName
                    )
                )
            }
        }
    }

    suspend fun voidTransaction(transaction: Transaction, reason: String, staffName: String) = withContext(Dispatchers.IO) {
        transactionDao.updateTransactionStatus(transaction.id, "VOID")
        val items = transactionDao.getItemsForTransaction(transaction.id)
        for (item in items) {
            val p = productDao.getProductById(item.productId)
            if (p != null) {
                val oldStock = p.stock
                val newStock = oldStock + item.quantity
                productDao.updateStock(p.id, newStock)
                stockDao.insertMovement(
                    StockMovement(
                        productId = p.id,
                        productName = p.name,
                        movementType = "VOID",
                        quantity = item.quantity,
                        previousStock = oldStock,
                        newStock = newStock,
                        note = "Void #${transaction.transactionNo}: $reason",
                        staffName = staffName
                    )
                )
            }
        }
    }

    suspend fun updateKitchenStatus(transactionId: Long, newStatus: String) = withContext(Dispatchers.IO) {
        transactionDao.updateKitchenStatus(transactionId, newStatus)
    }

    // Cash Management
    suspend fun openCashShift(staffName: String, initialCash: Double): Long = withContext(Dispatchers.IO) {
        val shift = CashShift(
            staffName = staffName,
            openedAt = System.currentTimeMillis(),
            initialCash = initialCash,
            expectedCash = initialCash,
            isOpen = true
        )
        cashShiftDao.insertShift(shift)
    }

    suspend fun addCashEntry(shiftId: Long, type: String, amount: Double, note: String, staffName: String, category: String = "Operasional") = withContext(Dispatchers.IO) {
        val entry = CashEntry(
            cashShiftId = shiftId,
            entryType = type,
            amount = amount,
            category = category,
            note = note,
            staffName = staffName
        )
        cashShiftDao.insertCashEntry(entry)
        val shift = cashShiftDao.getShiftById(shiftId)
        if (shift != null) {
            val newPayIn = if (type == "PAY_IN") shift.totalPayIn + amount else shift.totalPayIn
            val newPayOut = if (type == "PAY_OUT") shift.totalPayOut + amount else shift.totalPayOut
            val expected = shift.initialCash + newPayIn - newPayOut + shift.totalCashSales
            cashShiftDao.updateShift(
                shift.copy(
                    totalPayIn = newPayIn,
                    totalPayOut = newPayOut,
                    expectedCash = expected
                )
            )
        }
    }

    suspend fun closeCashShift(shiftId: Long, actualCash: Double, notes: String) = withContext(Dispatchers.IO) {
        val shift = cashShiftDao.getShiftById(shiftId) ?: return@withContext
        val expected = shift.initialCash + shift.totalPayIn - shift.totalPayOut + shift.totalCashSales
        val diff = actualCash - expected
        cashShiftDao.updateShift(
            shift.copy(
                closedAt = System.currentTimeMillis(),
                expectedCash = expected,
                actualCash = actualCash,
                difference = diff,
                isOpen = false,
                notes = notes
            )
        )
    }

    fun getEntriesForShift(shiftId: Long): Flow<List<CashEntry>> = cashShiftDao.getEntriesForShift(shiftId)

    // Staff
    suspend fun authenticateStaff(pin: String): StaffUser? = withContext(Dispatchers.IO) {
        staffDao.getStaffByPin(pin)
    }

    suspend fun saveStaff(staff: StaffUser): Long = withContext(Dispatchers.IO) {
        if (staff.id == 0L) staffDao.insertStaff(staff)
        else {
            staffDao.updateStaff(staff)
            staff.id
        }
    }

    suspend fun deleteStaff(staff: StaffUser) = withContext(Dispatchers.IO) {
        staffDao.deleteStaff(staff)
    }

    // Store Settings
    suspend fun updateStoreSettings(settings: StoreSettings) = withContext(Dispatchers.IO) {
        settingsDao.saveSettings(settings)
    }

    suspend fun getStoreSettingsDirect(): StoreSettings = withContext(Dispatchers.IO) {
        settingsDao.getSettingsDirect() ?: StoreSettings()
    }

    // Full Local Database Backup to JSON
    suspend fun exportDatabaseToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "POLL POS")
        root.put("exportedAt", System.currentTimeMillis())

        // Categories
        val categories = categoryDao.getAllCategories().firstOrNull() ?: emptyList()
        val catArray = JSONArray()
        for (c in categories) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("iconName", c.iconName)
            obj.put("colorHex", c.colorHex)
            obj.put("sortOrder", c.sortOrder)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // Products
        val products = productDao.getAllProducts().firstOrNull() ?: emptyList()
        val prodArray = JSONArray()
        for (p in products) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("price", p.price)
            obj.put("costPrice", p.costPrice)
            obj.put("stock", p.stock)
            obj.put("minStock", p.minStock)
            obj.put("sku", p.sku)
            obj.put("barcode", p.barcode)
            obj.put("categoryId", p.categoryId)
            obj.put("unit", p.unit)
            obj.put("description", p.description)
            obj.put("variantsJson", p.variantsJson)
            obj.put("isActive", p.isActive)
            prodArray.put(obj)
        }
        root.put("products", prodArray)

        // Customers
        val customers = customerDao.getAllCustomers().firstOrNull() ?: emptyList()
        val custArray = JSONArray()
        for (cu in customers) {
            val obj = JSONObject()
            obj.put("id", cu.id)
            obj.put("name", cu.name)
            obj.put("phone", cu.phone)
            obj.put("address", cu.address)
            obj.put("notes", cu.notes)
            obj.put("totalSpent", cu.totalSpent)
            obj.put("totalTransactions", cu.totalTransactions)
            obj.put("discountPercent", cu.discountPercent)
            obj.put("createdAt", cu.createdAt)
            custArray.put(obj)
        }
        root.put("customers", custArray)

        // Transactions & items
        val transactions = transactionDao.getAllTransactions().firstOrNull() ?: emptyList()
        val txArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("transactionNo", tx.transactionNo)
            obj.put("timestamp", tx.timestamp)
            obj.put("customerId", tx.customerId ?: -1L)
            obj.put("customerName", tx.customerName)
            obj.put("customerPhone", tx.customerPhone)
            obj.put("cashierName", tx.cashierName)
            obj.put("subtotal", tx.subtotal)
            obj.put("discountAmount", tx.discountAmount)
            obj.put("discountNote", tx.discountNote)
            obj.put("taxAmount", tx.taxAmount)
            obj.put("taxRate", tx.taxRate)
            obj.put("serviceCharge", tx.serviceCharge)
            obj.put("total", tx.total)
            obj.put("paymentStatus", tx.paymentStatus)
            obj.put("paymentMethodsSummary", tx.paymentMethodsSummary)
            obj.put("changeAmount", tx.changeAmount)
            obj.put("notes", tx.notes)
            obj.put("tableNo", tx.tableNo)
            obj.put("kitchenStatus", tx.kitchenStatus)

            // Items
            val items = transactionDao.getItemsForTransaction(tx.id)
            val itemsArr = JSONArray()
            for (it in items) {
                val itObj = JSONObject()
                itObj.put("productId", it.productId)
                itObj.put("productName", it.productName)
                itObj.put("price", it.price)
                itObj.put("costPrice", it.costPrice)
                itObj.put("quantity", it.quantity)
                itObj.put("unit", it.unit)
                itObj.put("variantNote", it.variantNote)
                itObj.put("itemDiscount", it.itemDiscount)
                itObj.put("total", it.total)
                itemsArr.put(itObj)
            }
            obj.put("items", itemsArr)

            // Payments
            val payments = transactionDao.getPaymentsForTransaction(tx.id)
            val payArr = JSONArray()
            for (py in payments) {
                val pyObj = JSONObject()
                pyObj.put("paymentMethod", py.paymentMethod)
                pyObj.put("amount", py.amount)
                pyObj.put("referenceNo", py.referenceNo)
                payArr.put(pyObj)
            }
            obj.put("payments", payArr)

            txArray.put(obj)
        }
        root.put("transactions", txArray)

        // Settings
        val settings = settingsDao.getSettingsDirect()
        if (settings != null) {
            val setObj = JSONObject()
            setObj.put("storeName", settings.storeName)
            setObj.put("address", settings.address)
            setObj.put("phone", settings.phone)
            setObj.put("taxRate", settings.taxRate)
            setObj.put("enableTax", settings.enableTax)
            setObj.put("serviceChargeRate", settings.serviceChargeRate)
            setObj.put("enableServiceCharge", settings.enableServiceCharge)
            setObj.put("paperSize", settings.paperSize)
            setObj.put("receiptHeader", settings.receiptHeader)
            setObj.put("receiptFooter", settings.receiptFooter)
            root.put("settings", setObj)
        }

        root.toString(2)
    }

    // Restore Database from JSON
    suspend fun restoreDatabaseFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            // Restore Categories
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                val categories = mutableListOf<Category>()
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    categories.add(
                        Category(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            iconName = obj.optString("iconName", "restaurant"),
                            colorHex = obj.optString("colorHex", "#C62828"),
                            sortOrder = obj.optInt("sortOrder", i)
                        )
                    )
                }
                categoryDao.insertAll(categories)
            }

            // Restore Products
            if (root.has("products")) {
                val prodArray = root.getJSONArray("products")
                val products = mutableListOf<Product>()
                for (i in 0 until prodArray.length()) {
                    val obj = prodArray.getJSONObject(i)
                    products.add(
                        Product(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            price = obj.getDouble("price"),
                            costPrice = obj.optDouble("costPrice", 0.0),
                            stock = obj.optDouble("stock", 0.0),
                            minStock = obj.optDouble("minStock", 5.0),
                            sku = obj.optString("sku", ""),
                            barcode = obj.optString("barcode", ""),
                            categoryId = obj.optLong("categoryId", 1L),
                            unit = obj.optString("unit", "pcs"),
                            description = obj.optString("description", ""),
                            variantsJson = obj.optString("variantsJson", ""),
                            isActive = obj.optBoolean("isActive", true)
                        )
                    )
                }
                productDao.insertAll(products)
            }

            // Restore Customers
            if (root.has("customers")) {
                val custArray = root.getJSONArray("customers")
                val customers = mutableListOf<Customer>()
                for (i in 0 until custArray.length()) {
                    val obj = custArray.getJSONObject(i)
                    customers.add(
                        Customer(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            phone = obj.getString("phone"),
                            address = obj.optString("address", ""),
                            notes = obj.optString("notes", ""),
                            totalSpent = obj.optDouble("totalSpent", 0.0),
                            totalTransactions = obj.optInt("totalTransactions", 0),
                            discountPercent = obj.optDouble("discountPercent", 0.0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                customerDao.insertAll(customers)
            }

            // Restore Settings
            if (root.has("settings")) {
                val setObj = root.getJSONObject("settings")
                val settings = StoreSettings(
                    storeName = setObj.optString("storeName", "POLL Cafe & Resto"),
                    address = setObj.optString("address", "Jl. Merdeka No. 45"),
                    phone = setObj.optString("phone", "0812-3456-7890"),
                    taxRate = setObj.optDouble("taxRate", 10.0),
                    enableTax = setObj.optBoolean("enableTax", false),
                    serviceChargeRate = setObj.optDouble("serviceChargeRate", 5.0),
                    enableServiceCharge = setObj.optBoolean("enableServiceCharge", false),
                    paperSize = setObj.optString("paperSize", "58mm"),
                    receiptHeader = setObj.optString("receiptHeader", "POLL POS"),
                    receiptFooter = setObj.optString("receiptFooter", "Terima Kasih")
                )
                settingsDao.saveSettings(settings)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
