package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.PollPosApplication
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.StockMovement
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * BaseCartInventoryViewModel
 *
 * Base ViewModel extending [CartViewModel] to handle reactive local state management for:
 * 1. Shopping Cart, Totals, Discounts, and Taxes (inherited from [CartViewModel])
 * 2. Inventory (product catalog, stock levels, low-stock alerts, categories, stock adjustments)
 *
 * Exposes reactive Kotlin StateFlows allowing the UI to react immediately to local data updates.
 */
open class BaseCartInventoryViewModel(application: Application) : CartViewModel(application) {

    protected val repository = (application as PollPosApplication).repository

    // =========================================================================
    // UI FEEDBACK & SNACKBAR
    // =========================================================================
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // =========================================================================
    // INVENTORY REACTIVE STATE (Room Database Local Storage)
    // =========================================================================
    val allProducts: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProducts: StateFlow<List<Product>> = repository.activeProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStockMovements: StateFlow<List<StockMovement>> = repository.allStockMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived Inventory Metrics
    val totalProductsCount: StateFlow<Int> = allProducts
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lowStockCount: StateFlow<Int> = lowStockProducts
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val outOfStockProducts: StateFlow<List<Product>> = allProducts
        .map { list -> list.filter { it.stock <= 0 && it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outOfStockCount: StateFlow<Int> = outOfStockProducts
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Filter & Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Long?>(null) // null = all
    val selectedCategory: StateFlow<Long?> = _selectedCategory.asStateFlow()

    val filteredProducts: StateFlow<List<Product>> = combine(activeProducts, _selectedCategory, _searchQuery) { products, catId, query ->
        products.filter { p ->
            val matchesCategory = catId == null || p.categoryId == catId
            val matchesSearch = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.sku.contains(query, ignoreCase = true) ||
                    p.barcode.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(catId: Long?) {
        _selectedCategory.value = catId
    }

    // Inventory CRUD operations
    fun saveProduct(product: Product, onComplete: ((Product) -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveProduct(product)
            showSnackbar("Produk ${product.name} berhasil disimpan")
            onComplete?.invoke(product)
        }
    }

    fun deleteProduct(product: Product, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            showSnackbar("Produk ${product.name} berhasil dihapus")
            onComplete?.invoke()
        }
    }

    fun saveCategory(category: Category, onComplete: ((Category) -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveCategory(category)
            showSnackbar("Kategori ${category.name} berhasil disimpan")
            onComplete?.invoke(category)
        }
    }

    fun deleteCategory(category: Category, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            showSnackbar("Kategori ${category.name} berhasil dihapus")
            onComplete?.invoke()
        }
    }

    open fun adjustStock(
        productId: Long,
        productName: String,
        type: String,
        qty: Double,
        staffName: String = "Admin",
        note: String = ""
    ) {
        viewModelScope.launch {
            repository.adjustStock(productId, productName, type, qty, staffName, note)
            showSnackbar("Stok $productName berhasil disesuaikan")
        }
    }

    open fun adjustStock(
        productId: Long,
        productName: String,
        type: String,
        qty: Double,
        note: String
    ) {
        adjustStock(productId, productName, type, qty, "Admin", note)
    }
}
