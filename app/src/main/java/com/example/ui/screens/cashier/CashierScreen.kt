package com.example.ui.screens.cashier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.ui.components.ProductImageView
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryRed
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.PosViewModel
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierScreen(
    viewModel: PosViewModel
) {
    val activeProducts by viewModel.activeProducts.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val allCustomers by viewModel.allCustomers.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val orderTableNo by viewModel.orderTableNo.collectAsState()
    val orderNotes by viewModel.orderNotes.collectAsState()
    val discountPercent by viewModel.discountPercent.collectAsState()
    val discountNominal by viewModel.discountNominal.collectAsState()
    val discountNote by viewModel.discountNote.collectAsState()

    val subtotal by viewModel.cartSubtotal.collectAsState()
    val discountAmount by viewModel.cartDiscountAmount.collectAsState()
    val taxAmount by viewModel.cartTaxAmount.collectAsState()
    val serviceCharge by viewModel.cartServiceChargeAmount.collectAsState()
    val grandTotal by viewModel.cartGrandTotal.collectAsState()

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 680

    var showMobileCartSheet by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var productForVariantModal by remember { mutableStateOf<Product?>(null) }

    // Filter products
    val filteredProducts = activeProducts.filter { p ->
        val matchesCategory = selectedCategory == null || p.categoryId == selectedCategory
        val matchesSearch = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.sku.contains(searchQuery, ignoreCase = true) ||
                p.barcode.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    if (isTablet) {
        // Tablet Split View: Products on Left (60%), Cart on Right (40%)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(modifier = Modifier.weight(0.60f).fillMaxHeight()) {
                ProductCatalogPane(
                    products = filteredProducts,
                    categories = allCategories,
                    cart = cart,
                    selectedCategoryId = selectedCategory,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onSelectCategory = { viewModel.selectCategory(it) },
                    onProductClick = { product ->
                        if (product.variantsJson.isNotBlank() && product.variantsJson != "[]") {
                            productForVariantModal = product
                        } else {
                            viewModel.addToCart(product)
                        }
                    }
                )
            }

            Surface(
                modifier = Modifier
                    .weight(0.40f)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp
            ) {
                CartPane(
                    cart = cart,
                    selectedCustomer = selectedCustomer,
                    orderTableNo = orderTableNo,
                    orderNotes = orderNotes,
                    subtotal = subtotal,
                    discountAmount = discountAmount,
                    taxAmount = taxAmount,
                    serviceCharge = serviceCharge,
                    grandTotal = grandTotal,
                    discountNote = discountNote,
                    onCustomerClick = { showCustomerPicker = true },
                    onTableChange = { viewModel.setOrderTableNo(it) },
                    onNotesChange = { viewModel.setOrderNotes(it) },
                    onDiscountClick = { showDiscountDialog = true },
                    onUpdateQty = { item, qty -> viewModel.updateCartQuantity(item, qty) },
                    onRemoveItem = { item -> viewModel.removeFromCart(item) },
                    onClearCart = { viewModel.clearCart() },
                    onCheckout = { showCheckoutDialog = true }
                )
            }
        }
    } else {
        // Mobile Handheld View: Products Grid with Floating Cart Bar / BottomSheet
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ProductCatalogPane(
                    products = filteredProducts,
                    categories = allCategories,
                    cart = cart,
                    selectedCategoryId = selectedCategory,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onSelectCategory = { viewModel.selectCategory(it) },
                    onProductClick = { product ->
                        if (product.variantsJson.isNotBlank() && product.variantsJson != "[]") {
                            productForVariantModal = product
                        } else {
                            viewModel.addToCart(product)
                        }
                    }
                )
            }

            // Floating Bottom Cart Bar for Phone
            if (cart.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(14.dp)
                        .clickable { showMobileCartSheet = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(
                                containerColor = Color.White,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = cart.sumOf { it.quantity }.toInt().toString(),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Keranjang Belanja",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = formatRp(grandTotal),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Lihat Keranjang",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Mobile Cart BottomSheet
    if (showMobileCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMobileCartSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                CartPane(
                    cart = cart,
                    selectedCustomer = selectedCustomer,
                    orderTableNo = orderTableNo,
                    orderNotes = orderNotes,
                    subtotal = subtotal,
                    discountAmount = discountAmount,
                    taxAmount = taxAmount,
                    serviceCharge = serviceCharge,
                    grandTotal = grandTotal,
                    discountNote = discountNote,
                    onCustomerClick = {
                        showMobileCartSheet = false
                        showCustomerPicker = true
                    },
                    onTableChange = { viewModel.setOrderTableNo(it) },
                    onNotesChange = { viewModel.setOrderNotes(it) },
                    onDiscountClick = {
                        showMobileCartSheet = false
                        showDiscountDialog = true
                    },
                    onUpdateQty = { item, qty -> viewModel.updateCartQuantity(item, qty) },
                    onRemoveItem = { item -> viewModel.removeFromCart(item) },
                    onClearCart = {
                        viewModel.clearCart()
                        showMobileCartSheet = false
                    },
                    onCheckout = {
                        showMobileCartSheet = false
                        showCheckoutDialog = true
                    }
                )
            }
        }
    }

    // Variant Selection Modal Dialog
    if (productForVariantModal != null) {
        val prod = productForVariantModal!!
        ProductVariantSelectionDialog(
            product = prod,
            onDismiss = { productForVariantModal = null },
            onConfirm = { variants, extraPrice, notes ->
                viewModel.addToCart(prod, variants, extraPrice, notes)
                productForVariantModal = null
            }
        )
    }

    // Customer Picker Dialog
    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = allCustomers,
            selectedCustomer = selectedCustomer,
            onDismiss = { showCustomerPicker = false },
            onSelect = { cust ->
                viewModel.setCustomer(cust)
                showCustomerPicker = false
            }
        )
    }

    // Discount Configuration Dialog
    if (showDiscountDialog) {
        DiscountDialog(
            currentPercent = discountPercent,
            currentNominal = discountNominal,
            subtotal = subtotal,
            onDismiss = { showDiscountDialog = false },
            onApplyPercent = { pct, note ->
                viewModel.applyPercentDiscount(pct, note)
                showDiscountDialog = false
            },
            onApplyNominal = { nom, note ->
                viewModel.applyNominalDiscount(nom, note)
                showDiscountDialog = false
            },
            onClear = {
                viewModel.clearDiscount()
                showDiscountDialog = false
            }
        )
    }

    // Checkout Sheet (Payments & Split Payment)
    if (showCheckoutDialog) {
        CheckoutDialog(
            viewModel = viewModel,
            totalAmount = grandTotal,
            onDismiss = { showCheckoutDialog = false },
            onSuccess = {
                showCheckoutDialog = false
            }
        )
    }
}

@Composable
fun ProductCatalogPane(
    products: List<Product>,
    categories: List<com.example.data.model.Category>,
    cart: List<CartItem> = emptyList(),
    selectedCategoryId: Long?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (Long?) -> Unit,
    onProductClick: (Product) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari produk, SKU, barcode...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari", tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips with Icons
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { onSelectCategory(null) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selectedCategoryId == null) Color.White else MaterialTheme.colorScheme.primary
                        )
                    },
                    label = { Text("Semua", fontWeight = if (selectedCategoryId == null) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
            items(categories) { cat ->
                val isSelected = selectedCategoryId == cat.id
                val icon = getCategoryIcon(cat.name)
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectCategory(cat.id) },
                    leadingIcon = {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                        )
                    },
                    label = { Text(cat.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Product Grid with HD 1:1 Images
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tidak ada produk yang sesuai",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 155.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 85.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    val catName = categories.firstOrNull { it.id == product.categoryId }?.name ?: ""
                    val inCartQty = cart.firstOrNull { it.product.id == product.id }?.quantity ?: 0.0
                    ProductCard(
                        product = product,
                        categoryName = catName,
                        inCartQty = inCartQty,
                        onClick = { onProductClick(product) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    categoryName: String = "",
    inCartQty: Double = 0.0,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
    ) {
        Column {
            // HD 1:1 Aspect Ratio Image Container with Overlaid Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                ProductImageView(
                    imageUri = product.imageUri,
                    productName = product.name,
                    categoryName = categoryName,
                    cornerRadius = 14,
                    modifier = Modifier.fillMaxSize()
                )

                // Stock Badge (Glassmorphic Top-Start)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (product.stock <= 0) Color.Red else if (product.stock <= product.minStock) AccentOrange else Color(0xFF1B5E20)).copy(alpha = 0.88f),
                    modifier = Modifier
                        .padding(7.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = if (product.stock <= 0) "Habis" else "${product.stock.toInt()} ${product.unit}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Variants Indicator Badge (Top-End)
                if (product.variantsJson.isNotBlank() && product.variantsJson != "[]") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        modifier = Modifier
                            .padding(7.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Varian",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // In-Cart Active Badge (Bottom-End over image)
                if (inCartQty > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = "${inCartQty.toInt()}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Info Body Below 1:1 Image
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatRp(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCategoryIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    val lower = name.lowercase()
    return when {
        lower.contains("kopi") || lower.contains("minum") || lower.contains("drink") -> Icons.Default.LocalCafe
        lower.contains("makan") || lower.contains("food") -> Icons.Default.Restaurant
        lower.contains("snack") || lower.contains("roti") || lower.contains("camilan") -> Icons.Default.BakeryDining
        lower.contains("paket") || lower.contains("hemat") -> Icons.Default.Sell
        lower.contains("retail") || lower.contains("umkm") -> Icons.Default.Storefront
        else -> Icons.Default.Category
    }
}

@Composable
fun CartPane(
    cart: List<CartItem>,
    selectedCustomer: Customer?,
    orderTableNo: String,
    orderNotes: String,
    subtotal: Double,
    discountAmount: Double,
    taxAmount: Double,
    serviceCharge: Double,
    grandTotal: Double,
    discountNote: String,
    onCustomerClick: () -> Unit,
    onTableChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDiscountClick: () -> Unit,
    onUpdateQty: (CartItem, Double) -> Unit,
    onRemoveItem: (CartItem) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Customer & Table Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onCustomerClick,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedCustomer?.name ?: "Pelanggan Umum",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }

            if (cart.isNotEmpty()) {
                IconButton(onClick = onClearCart) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Kosongkan Keranjang", tint = Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Table & Notes row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = orderTableNo,
                onValueChange = onTableChange,
                placeholder = { Text("No Meja", fontSize = 12.sp) },
                modifier = Modifier.weight(0.4f),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
            OutlinedTextField(
                value = orderNotes,
                onValueChange = onNotesChange,
                placeholder = { Text("Catatan pesanan...", fontSize = 12.sp) },
                modifier = Modifier.weight(0.6f),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
        }

        Divider(modifier = Modifier.padding(vertical = 10.dp))

        // Cart Items List
        if (cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Keranjang Masih Kosong",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Pilih produk dari daftar di samping",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cart) { item ->
                    CartItemRow(
                        item = item,
                        onIncrement = { onUpdateQty(item, item.quantity + 1.0) },
                        onDecrement = { onUpdateQty(item, item.quantity - 1.0) },
                        onRemove = { onRemoveItem(item) }
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Bill Summary
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatRp(subtotal), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            // Discount Row with click to edit
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDiscountClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (discountAmount > 0) "Diskon ($discountNote)" else "Tambah Diskon / Voucher",
                        fontSize = 13.sp,
                        color = if (discountAmount > 0) AccentOrange else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = if (discountAmount > 0) "-${formatRp(discountAmount)}" else "Rp0",
                    fontSize = 13.sp,
                    color = if (discountAmount > 0) AccentOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (taxAmount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Pajak", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatRp(taxAmount), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (serviceCharge > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Service Charge", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatRp(serviceCharge), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Pembayaran",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatRp(grandTotal),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Checkout Button
        Button(
            onClick = onCheckout,
            enabled = cart.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Payment, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Bayar ${formatRp(grandTotal)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                if (item.selectedVariants.isNotEmpty()) {
                    Text(
                        text = item.variantSummary,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (item.notes.isNotBlank()) {
                    Text(
                        text = "Catatan: ${item.notes}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatRp(item.unitPrice),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Decrement / Remove button
                IconButton(
                    onClick = { if (item.quantity <= 1) onRemove() else onDecrement() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (item.quantity <= 1) Icons.Default.Delete else Icons.Default.Remove,
                        contentDescription = "Kurang",
                        tint = if (item.quantity <= 1) Color.Red else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// Variant Selection Dialog
@Composable
fun ProductVariantSelectionDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>, Double, String) -> Unit
) {
    val selectedVariants = remember { mutableStateMapOf<String, String>() }
    var extraTotal by remember { mutableStateOf(0.0) }
    var itemNote by remember { mutableStateOf("") }

    // Parse variants JSON safely
    val variantGroups = remember(product) {
        val list = mutableListOf<com.example.data.model.ProductVariantGroup>()
        try {
            val arr = JSONArray(product.variantsJson)
            for (i in 0 until arr.length()) {
                val groupObj = arr.getJSONObject(i)
                val groupName = groupObj.getString("name")
                val optArr = groupObj.getJSONArray("options")
                val options = mutableListOf<com.example.data.model.ProductVariantOption>()
                for (j in 0 until optArr.length()) {
                    val optObj = optArr.getJSONObject(j)
                    options.add(
                        com.example.data.model.ProductVariantOption(
                            name = optObj.getString("name"),
                            extraPrice = optObj.optDouble("extraPrice", 0.0)
                        )
                    )
                }
                list.add(com.example.data.model.ProductVariantGroup(groupName, options))
            }
        } catch (_: Exception) {}
        list
    }

    // Set defaults
    LaunchedEffect(variantGroups) {
        variantGroups.forEach { grp ->
            if (!selectedVariants.containsKey(grp.name) && grp.options.isNotEmpty()) {
                selectedVariants[grp.name] = grp.options[0].name
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.95f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Harga dasar: ${formatRp(product.price)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                for (grp in variantGroups) {
                    Text(
                        text = grp.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(grp.options) { opt ->
                            val isSelected = selectedVariants[grp.name] == opt.name
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedVariants[grp.name] = opt.name
                                },
                                label = {
                                    val priceExtra = if (opt.extraPrice > 0) " (+${formatRp(opt.extraPrice)})" else ""
                                    Text("${opt.name}$priceExtra")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = itemNote,
                    onValueChange = { itemNote = it },
                    placeholder = { Text("Catatan khusus (misal: jangan pedas)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(selectedVariants.toMap(), extraTotal, itemNote)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Tambahkan")
                    }
                }
            }
        }
    }
}

// Customer Picker Dialog
@Composable
fun CustomerPickerDialog(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    onDismiss: () -> Unit,
    onSelect: (Customer?) -> Unit
) {
    var searchCust by remember { mutableStateOf("") }

    val filtered = customers.filter {
        searchCust.isBlank() || it.name.contains(searchCust, ignoreCase = true) || it.phone.contains(searchCust)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.7f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Pilih Pelanggan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchCust,
                    onValueChange = { searchCust = it },
                    placeholder = { Text("Cari nama atau nomor HP...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            onClick = { onSelect(null) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCustomer == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PersonOutline, contentDescription = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Pelanggan Umum (Tanpa Nama)", fontWeight = FontWeight.Bold)
                                    Text("Non-member", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    items(filtered) { cust ->
                        val isSelected = selectedCustomer?.id == cust.id
                        Card(
                            onClick = { onSelect(cust) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(cust.name, fontWeight = FontWeight.Bold)
                                    Text(cust.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (cust.discountPercent > 0) {
                                        Text("Diskon Member: ${cust.discountPercent.toInt()}%", fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Text("Total: ${formatRp(cust.totalSpent)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

// Discount Dialog
@Composable
fun DiscountDialog(
    currentPercent: Double,
    currentNominal: Double,
    subtotal: Double,
    onDismiss: () -> Unit,
    onApplyPercent: (Double, String) -> Unit,
    onApplyNominal: (Double, String) -> Unit,
    onClear: () -> Unit
) {
    var mode by remember { mutableStateOf(if (currentNominal > 0) "NOMINAL" else "PERCENT") }
    var inputVal by remember { mutableStateOf(if (currentNominal > 0) currentNominal.toLong().toString() else if (currentPercent > 0) currentPercent.toInt().toString() else "") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
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
                Text("Atur Diskon Transaksi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = mode == "PERCENT",
                        onClick = { mode = "PERCENT"; inputVal = "" },
                        label = { Text("Persentase (%)") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = mode == "NOMINAL",
                        onClick = { mode = "NOMINAL"; inputVal = "" },
                        label = { Text("Nominal (Rp)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (mode == "PERCENT") {
                    // Quick Percent buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5.0, 10.0, 15.0, 20.0).forEach { pct ->
                            OutlinedButton(
                                onClick = { inputVal = pct.toInt().toString() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text("${pct.toInt()}%")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = inputVal,
                    onValueChange = { inputVal = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (mode == "PERCENT") "Diskon (%)" else "Potongan Harga (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Keterangan (misal: Promo Akhir Pekan)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onClear) {
                        Text("Hapus Diskon", color = Color.Red)
                    }

                    Row {
                        TextButton(onClick = onDismiss) { Text("Batal") }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                val num = inputVal.toDoubleOrNull() ?: 0.0
                                if (mode == "PERCENT") {
                                    onApplyPercent(num, note)
                                } else {
                                    onApplyNominal(num, note)
                                }
                            }
                        ) {
                            Text("Terapkan")
                        }
                    }
                }
            }
        }
    }
}

// Checkout Dialog (Supports Cash Quick Bills & Split Payment)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutDialog(
    viewModel: PosViewModel,
    totalAmount: Double,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var isSplitPayment by remember { mutableStateOf(false) }
    var selectedSingleMethod by remember { mutableStateOf("Tunai") }
    var cashTenderedInput by remember { mutableStateOf("") }
    var splitAmountInput by remember { mutableStateOf("") }
    var splitMethodSelected by remember { mutableStateOf("Tunai") }

    val splitPayments by viewModel.splitPayments.collectAsState()
    val totalPaidSplit = splitPayments.sumOf { it.amount }
    val remainingSplit = (totalAmount - totalPaidSplit).coerceAtLeast(0.0)

    val methods = listOf(
        "Tunai", "QRIS", "BCA", "BRI", "BNI", "Mandiri", "GoPay", "OVO", "DANA", "Debit", "Kredit"
    )

    val tenderedNum = cashTenderedInput.toDoubleOrNull() ?: totalAmount
    val change = if (selectedSingleMethod == "Tunai") (tenderedNum - totalAmount).coerceAtLeast(0.0) else 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Pembayaran", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Total: ${formatRp(totalAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle Single Payment vs Split Payment
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = !isSplitPayment,
                        onClick = { isSplitPayment = false },
                        label = { Text("Metode Tunggal") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = isSplitPayment,
                        onClick = { isSplitPayment = true },
                        label = { Text("Split Payment") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!isSplitPayment) {
                    // Single Payment Mode
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text("Pilih Cara Bayar:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(methods) { m ->
                                    FilterChip(
                                        selected = selectedSingleMethod == m,
                                        onClick = { selectedSingleMethod = m },
                                        label = { Text(m) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        if (selectedSingleMethod == "Tunai") {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Uang Diterima:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))

                                // Quick Cash Buttons
                                val quickAmounts = listOf(
                                    totalAmount,
                                    10000.0,
                                    20000.0,
                                    50000.0,
                                    100000.0,
                                    200000.0
                                ).filter { it >= totalAmount || it == totalAmount }.distinct()

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(quickAmounts) { amt ->
                                        OutlinedButton(
                                            onClick = { cashTenderedInput = amt.toLong().toString() },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(if (amt == totalAmount) "Uang Pas" else formatRp(amt), fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = cashTenderedInput,
                                    onValueChange = { cashTenderedInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Jumlah Uang Tunai (Rp)") },
                                    placeholder = { Text(totalAmount.toLong().toString()) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Change Display
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Kembalian", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = formatRp(change),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = AccentGreen
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Menunggu Pembayaran $selectedSingleMethod",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Tagihan: ${formatRp(totalAmount)}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.processCheckout(
                                singlePaymentMethod = selectedSingleMethod,
                                tenderedCash = tenderedNum,
                                onSuccess = { onSuccess() }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Selesaikan Pembayaran & Struk", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    // Split Payment Mode
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = if (remainingSplit > 0) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Sisa Belum Dibayar:", fontSize = 12.sp)
                                        Text(
                                            formatRp(remainingSplit),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = if (remainingSplit > 0) Color(0xFFE65100) else AccentGreen
                                        )
                                    }
                                    Text("Terbayar: ${formatRp(totalPaidSplit)}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        // Added split payments list
                        if (splitPayments.isNotEmpty()) {
                            item {
                                Text("Metode Terpilih:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            items(splitPayments) { entry ->
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${entry.method}: ${formatRp(entry.amount)}", fontWeight = FontWeight.SemiBold)
                                        IconButton(onClick = { viewModel.removeSplitPayment(entry) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Hapus", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Add next split payment section
                        if (remainingSplit > 0) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tambah Pembayaran Berikutnya:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(methods) { m ->
                                        FilterChip(
                                            selected = splitMethodSelected == m,
                                            onClick = { splitMethodSelected = m },
                                            label = { Text(m) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = splitAmountInput,
                                        onValueChange = { splitAmountInput = it.filter { ch -> ch.isDigit() } },
                                        placeholder = { Text(remainingSplit.toLong().toString()) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = {
                                            val amt = splitAmountInput.toDoubleOrNull() ?: remainingSplit
                                            if (amt > 0) {
                                                viewModel.addSplitPayment(splitMethodSelected, amt)
                                                splitAmountInput = ""
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Tambah")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.processCheckout(
                                singlePaymentMethod = null,
                                onSuccess = { onSuccess() }
                            )
                        },
                        enabled = remainingSplit <= 0 && splitPayments.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Selesaikan Split Payment & Struk", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
