package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PollPosApplication
import com.example.data.model.Customer
import com.example.data.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Representation of an individual item in the shopping cart.
 */
data class CartItem(
    val product: Product,
    val quantity: Double = 1.0,
    val selectedVariants: Map<String, String> = emptyMap(),
    val extraPrice: Double = 0.0,
    val notes: String = ""
) {
    val unitPrice: Double get() = product.price + extraPrice
    val totalPrice: Double get() = unitPrice * quantity
    val variantSummary: String get() = selectedVariants.entries.joinToString(", ") { "${it.key}: ${it.value}" }
}

/**
 * CartViewModel
 *
 * Dedicated ViewModel using Kotlin [StateFlow] to manage shopping cart items,
 * compute reactive totals (subtotal, discount, tax, service charge, grand total),
 * and handle discount and tax operations in real-time.
 *
 * Designed to be observable and lifecycle-aware for Jetpack Compose UI.
 */
open class CartViewModel(application: Application) : AndroidViewModel(application) {

    // =========================================================================
    // CART ITEMS STATE
    // =========================================================================
    protected val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()
    val cartItems: StateFlow<List<CartItem>> get() = cart

    // Order Context (Customer, Table, Notes)
    protected val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    protected val _orderTableNo = MutableStateFlow("")
    val orderTableNo: StateFlow<String> = _orderTableNo.asStateFlow()
    val tableNo: StateFlow<String> get() = orderTableNo

    protected val _orderNotes = MutableStateFlow("")
    val orderNotes: StateFlow<String> = _orderNotes.asStateFlow()
    val notes: StateFlow<String> get() = orderNotes

    // =========================================================================
    // DISCOUNT STATE
    // =========================================================================
    protected val _discountPercent = MutableStateFlow(0.0)
    val discountPercent: StateFlow<Double> = _discountPercent.asStateFlow()

    protected val _discountNominal = MutableStateFlow(0.0)
    val discountNominal: StateFlow<Double> = _discountNominal.asStateFlow()

    protected val _discountNote = MutableStateFlow("")
    val discountNote: StateFlow<String> = _discountNote.asStateFlow()

    // =========================================================================
    // TAX & SERVICE CHARGE STATE
    // =========================================================================
    protected val _isTaxEnabled = MutableStateFlow(false)
    val isTaxEnabled: StateFlow<Boolean> = _isTaxEnabled.asStateFlow()

    protected val _taxRate = MutableStateFlow(0.0)
    val taxRate: StateFlow<Double> = _taxRate.asStateFlow()

    protected val _isServiceChargeEnabled = MutableStateFlow(false)
    val isServiceChargeEnabled: StateFlow<Boolean> = _isServiceChargeEnabled.asStateFlow()

    protected val _serviceChargeRate = MutableStateFlow(0.0)
    val serviceChargeRate: StateFlow<Double> = _serviceChargeRate.asStateFlow()

    // =========================================================================
    // REACTIVE COMPUTED TOTALS (Observable by UI)
    // =========================================================================
    private val _isCartEmpty = MutableStateFlow(true)
    val isCartEmpty: StateFlow<Boolean> = _isCartEmpty.asStateFlow()
    val isEmpty: StateFlow<Boolean> get() = isCartEmpty

    private val _cartItemCount = MutableStateFlow(0)
    val cartItemCount: StateFlow<Int> = _cartItemCount.asStateFlow()
    val itemCount: StateFlow<Int> get() = cartItemCount

    private val _cartTotalQuantity = MutableStateFlow(0.0)
    val cartTotalQuantity: StateFlow<Double> = _cartTotalQuantity.asStateFlow()
    val totalQuantity: StateFlow<Double> get() = cartTotalQuantity

    private val _cartSubtotal = MutableStateFlow(0.0)
    val cartSubtotal: StateFlow<Double> = _cartSubtotal.asStateFlow()
    val subtotal: StateFlow<Double> get() = cartSubtotal

    private val _cartDiscountAmount = MutableStateFlow(0.0)
    val cartDiscountAmount: StateFlow<Double> = _cartDiscountAmount.asStateFlow()
    val discountAmount: StateFlow<Double> get() = cartDiscountAmount

    private val _taxableBase = MutableStateFlow(0.0)
    val taxableBase: StateFlow<Double> = _taxableBase.asStateFlow()

    private val _cartTaxAmount = MutableStateFlow(0.0)
    val cartTaxAmount: StateFlow<Double> = _cartTaxAmount.asStateFlow()
    val taxAmount: StateFlow<Double> get() = cartTaxAmount

    private val _cartServiceChargeAmount = MutableStateFlow(0.0)
    val cartServiceChargeAmount: StateFlow<Double> = _cartServiceChargeAmount.asStateFlow()
    val serviceChargeAmount: StateFlow<Double> get() = cartServiceChargeAmount

    private val _cartGrandTotal = MutableStateFlow(0.0)
    val cartGrandTotal: StateFlow<Double> = _cartGrandTotal.asStateFlow()
    val grandTotal: StateFlow<Double> get() = cartGrandTotal

    init {
        // Automatically sync initial tax and service charge rates from store settings if available
        if (application is PollPosApplication) {
            viewModelScope.launch {
                application.repository.storeSettings.filterNotNull().collect { settings ->
                    _taxRate.value = settings.taxRate
                    _isTaxEnabled.value = settings.enableTax
                    _serviceChargeRate.value = settings.serviceChargeRate
                    _isServiceChargeEnabled.value = settings.enableServiceCharge
                    recalculateTotals()
                }
            }
        }
    }

    /**
     * Recalculates all cart totals synchronously whenever items, discounts, or tax rates change.
     */
    protected fun recalculateTotals() {
        val items = _cart.value
        val sub = items.sumOf { it.totalPrice }
        _cartSubtotal.value = sub
        _cartItemCount.value = items.size
        _cartTotalQuantity.value = items.sumOf { it.quantity }
        _isCartEmpty.value = items.isEmpty()

        // Calculate discount
        val pct = _discountPercent.value
        val nom = _discountNominal.value
        val disc = if (nom > 0) nom.coerceAtMost(sub)
        else if (pct > 0) (sub * pct / 100.0).coerceAtMost(sub)
        else 0.0
        _cartDiscountAmount.value = disc

        // Calculate taxable base after discount
        val base = (sub - disc).coerceAtLeast(0.0)
        _taxableBase.value = base

        // Calculate tax
        val tax = if (_isTaxEnabled.value && _taxRate.value > 0) {
            base * _taxRate.value / 100.0
        } else {
            0.0
        }
        _cartTaxAmount.value = tax

        // Calculate service charge
        val sc = if (_isServiceChargeEnabled.value && _serviceChargeRate.value > 0) {
            base * _serviceChargeRate.value / 100.0
        } else {
            0.0
        }
        _cartServiceChargeAmount.value = sc

        // Grand Total
        _cartGrandTotal.value = (base + tax + sc).coerceAtLeast(0.0)
    }

    // =========================================================================
    // CART OPERATIONS
    // =========================================================================
    open fun addToCart(
        product: Product,
        quantity: Double = 1.0,
        variants: Map<String, String> = emptyMap(),
        extraPrice: Double = 0.0,
        notes: String = ""
    ) {
        if (quantity <= 0) return
        val currentList = _cart.value.toMutableList()
        val existingIndex = currentList.indexOfFirst {
            it.product.id == product.id && it.selectedVariants == variants && it.notes == notes
        }
        if (existingIndex >= 0) {
            val existing = currentList[existingIndex]
            currentList[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            currentList.add(CartItem(product, quantity, variants, extraPrice, notes))
        }
        _cart.value = currentList
        recalculateTotals()
    }

    open fun addToCart(
        product: Product,
        variants: Map<String, String>,
        extraPrice: Double,
        notes: String
    ) {
        addToCart(product, 1.0, variants, extraPrice, notes)
    }

    open fun updateCartQuantity(item: CartItem, newQty: Double) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOf(item)
        if (index >= 0) {
            if (newQty <= 0) {
                currentList.removeAt(index)
            } else {
                currentList[index] = item.copy(quantity = newQty)
            }
            _cart.value = currentList
            recalculateTotals()
        }
    }

    open fun updateQuantity(item: CartItem, newQty: Double) = updateCartQuantity(item, newQty)

    open fun incrementCartQuantity(item: CartItem, step: Double = 1.0) {
        updateCartQuantity(item, item.quantity + step)
    }

    open fun incrementQuantity(item: CartItem, step: Double = 1.0) = incrementCartQuantity(item, step)

    open fun decrementCartQuantity(item: CartItem, step: Double = 1.0) {
        updateCartQuantity(item, item.quantity - step)
    }

    open fun decrementQuantity(item: CartItem, step: Double = 1.0) = decrementCartQuantity(item, step)

    open fun removeFromCart(item: CartItem) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst {
            it.product.id == item.product.id &&
            it.selectedVariants == item.selectedVariants &&
            it.notes == item.notes
        }
        if (index >= 0) {
            currentList.removeAt(index)
            _cart.value = currentList
            recalculateTotals()
        }
    }

    open fun removeItem(item: CartItem) = removeFromCart(item)

    open fun clearCart() {
        _cart.value = emptyList()
        _selectedCustomer.value = null
        _orderTableNo.value = ""
        _orderNotes.value = ""
        _discountPercent.value = 0.0
        _discountNominal.value = 0.0
        _discountNote.value = ""
        recalculateTotals()
    }

    // =========================================================================
    // DISCOUNT OPERATIONS
    // =========================================================================
    open fun applyPercentDiscount(percent: Double, note: String = "") {
        _discountPercent.value = percent.coerceAtLeast(0.0)
        _discountNominal.value = 0.0
        _discountNote.value = if (note.isNotBlank()) note else "Diskon ${percent.toInt()}%"
        recalculateTotals()
    }

    open fun applyDiscountPercent(percent: Double, note: String = "") = applyPercentDiscount(percent, note)

    open fun applyNominalDiscount(nominal: Double, note: String = "") {
        _discountNominal.value = nominal.coerceAtLeast(0.0)
        _discountPercent.value = 0.0
        _discountNote.value = if (note.isNotBlank()) note else "Potongan Rp${nominal.toLong()}"
        recalculateTotals()
    }

    open fun applyDiscountNominal(nominal: Double, note: String = "") = applyNominalDiscount(nominal, note)

    open fun clearDiscount() {
        _discountPercent.value = 0.0
        _discountNominal.value = 0.0
        _discountNote.value = ""
        recalculateTotals()
    }

    // =========================================================================
    // TAX & SERVICE CHARGE OPERATIONS
    // =========================================================================
    open fun setTax(rate: Double, enabled: Boolean = true) {
        _taxRate.value = rate.coerceAtLeast(0.0)
        _isTaxEnabled.value = enabled
        recalculateTotals()
    }

    open fun setTaxRate(rate: Double) {
        _taxRate.value = rate.coerceAtLeast(0.0)
        recalculateTotals()
    }

    open fun setTaxEnabled(enabled: Boolean) {
        _isTaxEnabled.value = enabled
        recalculateTotals()
    }

    open fun setServiceCharge(rate: Double, enabled: Boolean = true) {
        _serviceChargeRate.value = rate.coerceAtLeast(0.0)
        _isServiceChargeEnabled.value = enabled
        recalculateTotals()
    }

    open fun setServiceChargeRate(rate: Double) {
        _serviceChargeRate.value = rate.coerceAtLeast(0.0)
        recalculateTotals()
    }

    open fun setServiceChargeEnabled(enabled: Boolean) {
        _isServiceChargeEnabled.value = enabled
        recalculateTotals()
    }

    // =========================================================================
    // CUSTOMER & ORDER CONTEXT
    // =========================================================================
    open fun setCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
        if (customer != null && customer.discountPercent > 0) {
            _discountPercent.value = customer.discountPercent
            _discountNote.value = "Diskon Member: ${customer.name} (${customer.discountPercent}%)"
        }
        recalculateTotals()
    }

    open fun setOrderTableNo(tableNo: String) {
        _orderTableNo.value = tableNo
    }

    open fun setTableNo(tableNo: String) = setOrderTableNo(tableNo)

    open fun setOrderNotes(notes: String) {
        _orderNotes.value = notes
    }

    open fun setNotes(notes: String) = setOrderNotes(notes)
}
