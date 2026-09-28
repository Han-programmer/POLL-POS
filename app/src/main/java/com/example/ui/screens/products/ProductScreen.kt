package com.example.ui.screens.products

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.components.ProductImageView
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatRp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryRed
import com.example.ui.viewmodel.PosViewModel
import com.example.util.rememberProductImagePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf<Long?>(null) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    val filteredProducts = allProducts.filter { p ->
        val matchesCategory = selectedCatId == null || p.categoryId == selectedCatId
        val matchesSearch = searchQuery.isBlank() ||
                p.name.contains(searchQuery, ignoreCase = true) ||
                p.sku.contains(searchQuery, ignoreCase = true) ||
                p.barcode.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddingNew = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
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
            // Top Row: Title & Export Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Katalog Produk (${filteredProducts.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = { viewModel.exportProductsCsv(context) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export CSV")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari produk, SKU, barcode...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCatId == null,
                        onClick = { selectedCatId = null },
                        label = { Text("Semua") }
                    )
                }
                items(allCategories) { cat ->
                    FilterChip(
                        selected = selectedCatId == cat.id,
                        onClick = { selectedCatId = cat.id },
                        label = { Text(cat.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Products List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada produk yang ditemukan",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredProducts) { prod ->
                        val catName = allCategories.firstOrNull { it.id == prod.categoryId }?.name ?: "Umum"
                        ProductRowCard(
                            product = prod,
                            categoryName = catName,
                            onEdit = { editingProduct = prod },
                            onDelete = { viewModel.deleteProduct(prod) }
                        )
                    }
                }
            }
        }
    }

    // Add or Edit Product Dialog
    if (isAddingNew || editingProduct != null) {
        val prodToEdit = editingProduct
        ProductFormDialog(
            product = prodToEdit,
            categories = allCategories,
            onDismiss = {
                isAddingNew = false
                editingProduct = null
            },
            onSave = { product ->
                viewModel.saveProduct(product)
                isAddingNew = false
                editingProduct = null
            }
        )
    }
}

@Composable
fun ProductRowCard(
    product: Product,
    categoryName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // HD 1:1 Product Image Thumbnail
            ProductImageView(
                imageUri = product.imageUri,
                productName = product.name,
                categoryName = categoryName,
                cornerRadius = 10,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$categoryName • SKU: ${if (product.sku.isNotBlank()) product.sku else "-"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatRp(product.price),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Modal: ${formatRp(product.costPrice)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(
                    text = "Stok: ${product.stock.toInt()} ${product.unit}",
                    color = if (product.stock <= 0) Color.Red else if (product.stock <= product.minStock) AccentOrange else AccentGreen
                )
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    product: Product?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var categoryId by remember { mutableStateOf(product?.categoryId ?: categories.firstOrNull()?.id ?: 1L) }
    var imageUri by remember { mutableStateOf(product?.imageUri ?: "") }
    var priceInput by remember { mutableStateOf(product?.price?.toLong()?.toString() ?: "") }
    var costPriceInput by remember { mutableStateOf(product?.costPrice?.toLong()?.toString() ?: "") }
    var stockInput by remember { mutableStateOf(product?.stock?.toInt()?.toString() ?: "0") }
    var minStockInput by remember { mutableStateOf(product?.minStock?.toInt()?.toString() ?: "5") }
    var sku by remember { mutableStateOf(product?.sku ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var unit by remember { mutableStateOf(product?.unit ?: "pcs") }
    var description by remember { mutableStateOf(product?.description ?: "") }

    // Helper utility using ActivityResultContracts.PickVisualMedia for local device storage
    val imagePicker = rememberProductImagePicker { localPath ->
        imageUri = localPath
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
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
                Text(
                    text = if (product == null) "Tambah Produk Baru" else "Edit Produk",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // HD 1:1 Image Customization Section with Local Storage Picker
                    Text("Foto Produk Menu (HD 1:1):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ProductImageView(
                                    imageUri = imageUri.ifBlank { null },
                                    productName = name.ifBlank { "Produk" },
                                    cornerRadius = 14,
                                    modifier = Modifier.size(92.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Primary Button: Pick from Local Storage / Gallery
                                    Button(
                                        onClick = { imagePicker.launch() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (imageUri.isNotBlank() && !imageUri.startsWith("res:"))
                                                "Ganti Foto Galeri HP"
                                            else
                                                "Pilih Foto dari Galeri HP/Tablet",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (imageUri.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = { imageUri = "" },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Hapus Foto",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            // Feedback message if image is from local device storage
                            val isLocalStorage = imageUri.isNotBlank() && (
                                imageUri.startsWith("/") ||
                                imageUri.contains("product_images") ||
                                imageUri.startsWith("file://")
                            )

                            if (isLocalStorage) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Foto dari Memori Lokal HP/Tablet (Offline)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "Tersimpan aman di penyimpanan internal perangkat",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Produk *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Category Selector
                    Text("Kategori:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = categoryId == cat.id,
                                onClick = { categoryId = cat.id },
                                label = { Text(cat.name) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Harga Jual (Rp) *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = costPriceInput,
                            onValueChange = { costPriceInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Harga Modal (Rp)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = stockInput,
                            onValueChange = { stockInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Stok") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = minStockInput,
                            onValueChange = { minStockInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Batas Min Stok") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Barcode") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan (pcs, porsi, cup, bungkus)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val p = Product(
                                    id = product?.id ?: 0L,
                                    name = name,
                                    categoryId = categoryId,
                                    imageUri = imageUri.ifBlank { null },
                                    price = priceInput.toDoubleOrNull() ?: 0.0,
                                    costPrice = costPriceInput.toDoubleOrNull() ?: 0.0,
                                    stock = stockInput.toDoubleOrNull() ?: 0.0,
                                    minStock = minStockInput.toDoubleOrNull() ?: 5.0,
                                    sku = sku,
                                    barcode = barcode,
                                    unit = if (unit.isNotBlank()) unit else "pcs",
                                    description = description,
                                    variantsJson = product?.variantsJson ?: ""
                                )
                                onSave(p)
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
