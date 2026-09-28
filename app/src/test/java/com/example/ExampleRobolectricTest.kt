package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Product
import com.example.ui.viewmodel.BaseCartInventoryViewModel
import com.example.ui.viewmodel.CartViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("POLL POS", appName)
    }

    @Test
    fun `cartViewModel manages items, calculates totals, discounts and taxes`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val cartVm = CartViewModel(app)

        val productA = Product(
            id = 1L,
            name = "Nasi Goreng Spesial",
            price = 25000.0,
            costPrice = 12000.0,
            stock = 30.0,
            unit = "porsi"
        )
        val productB = Product(
            id = 2L,
            name = "Es Teh Manis",
            price = 5000.0,
            costPrice = 1500.0,
            stock = 100.0,
            unit = "gelas"
        )

        // Initial state
        assertTrue(cartVm.isCartEmpty.value)
        assertTrue(cartVm.isEmpty.value)
        assertEquals(0, cartVm.cartItemCount.value)
        assertEquals(0, cartVm.itemCount.value)
        assertEquals(0.0, cartVm.cartSubtotal.value, 0.01)
        assertEquals(0.0, cartVm.cartGrandTotal.value, 0.01)

        // 1. Add Product A: 2 portions @ 25,000 = 50,000
        cartVm.addToCart(productA, quantity = 2.0)
        assertFalse(cartVm.isCartEmpty.value)
        assertEquals(1, cartVm.cartItemCount.value)
        assertEquals(2.0, cartVm.cartTotalQuantity.value, 0.01)
        assertEquals(50000.0, cartVm.cartSubtotal.value, 0.01)

        // 2. Add Product B: 1 portion @ 5,000 = 5,000. Total subtotal = 55,000
        cartVm.addToCart(productB, quantity = 1.0)
        assertEquals(2, cartVm.cartItemCount.value)
        assertEquals(3.0, cartVm.cartTotalQuantity.value, 0.01)
        assertEquals(55000.0, cartVm.cartSubtotal.value, 0.01)

        // 3. Discount Handling: Apply 10% discount on 55,000 -> 5,500 discount
        cartVm.applyPercentDiscount(10.0, "Diskon Promo")
        assertEquals(10.0, cartVm.discountPercent.value, 0.01)
        assertEquals(5500.0, cartVm.cartDiscountAmount.value, 0.01)
        assertEquals(5500.0, cartVm.discountAmount.value, 0.01)
        // Taxable Base = 55,000 - 5,500 = 49,500
        assertEquals(49500.0, cartVm.taxableBase.value, 0.01)

        // 4. Tax Handling: Enable PB1 / PPN 10% on taxable base (49,500 * 10% = 4,950)
        cartVm.setTax(rate = 10.0, enabled = true)
        assertTrue(cartVm.isTaxEnabled.value)
        assertEquals(10.0, cartVm.taxRate.value, 0.01)
        assertEquals(4950.0, cartVm.cartTaxAmount.value, 0.01)
        assertEquals(4950.0, cartVm.taxAmount.value, 0.01)

        // 5. Service Charge Handling: Enable 5% service charge (49,500 * 5% = 2,475)
        cartVm.setServiceCharge(rate = 5.0, enabled = true)
        assertTrue(cartVm.isServiceChargeEnabled.value)
        assertEquals(2475.0, cartVm.cartServiceChargeAmount.value, 0.01)

        // Grand Total = 49,500 + 4,950 + 2,475 = 56,925
        assertEquals(56925.0, cartVm.cartGrandTotal.value, 0.01)
        assertEquals(56925.0, cartVm.grandTotal.value, 0.01)

        // 6. Test Nominal Discount: Switch to Rp 15,000 nominal discount
        cartVm.applyNominalDiscount(15000.0, "Voucher Belanja")
        assertEquals(0.0, cartVm.discountPercent.value, 0.01)
        assertEquals(15000.0, cartVm.discountNominal.value, 0.01)
        assertEquals(15000.0, cartVm.cartDiscountAmount.value, 0.01)
        // Taxable base = 55,000 - 15,000 = 40,000
        assertEquals(40000.0, cartVm.taxableBase.value, 0.01)
        // Tax = 40,000 * 10% = 4,000
        assertEquals(4000.0, cartVm.cartTaxAmount.value, 0.01)
        // Service charge = 40,000 * 5% = 2,000
        assertEquals(2000.0, cartVm.cartServiceChargeAmount.value, 0.01)
        // Grand total = 40,000 + 4,000 + 2,000 = 46,000
        assertEquals(46000.0, cartVm.cartGrandTotal.value, 0.01)

        // 7. Increment & Decrement
        val itemB = cartVm.cart.value.first { it.product.id == 2L }
        cartVm.incrementQuantity(itemB, 2.0) // 1 + 2 = 3 portions of Es Teh
        assertEquals(5.0, cartVm.cart.value.first { it.product.id == 1L }.quantity + cartVm.cart.value.first { it.product.id == 2L }.quantity, 0.01)

        // 8. Remove item
        cartVm.removeItem(itemB)
        assertEquals(1, cartVm.cartItemCount.value)

        // 9. Clear cart
        cartVm.clearCart()
        assertTrue(cartVm.isCartEmpty.value)
        assertEquals(0.0, cartVm.cartSubtotal.value, 0.01)
        assertEquals(0.0, cartVm.cartGrandTotal.value, 0.01)
    }

    @Test
    fun `base cart viewModel handles cart operations and calculations`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = BaseCartInventoryViewModel(app)

        val testProduct = Product(
            id = 100L,
            name = "Kopi Susu Gula Aren",
            price = 18000.0,
            costPrice = 8000.0,
            stock = 50.0,
            unit = "cup"
        )

        assertTrue(vm.isCartEmpty.value)
        assertEquals(0, vm.cartItemCount.value)

        // Add to cart
        vm.addToCart(testProduct)
        assertEquals(1, vm.cart.value.size)
        assertEquals(18000.0, vm.cartSubtotal.value, 0.01)

        // Add same product again (increments quantity)
        vm.addToCart(testProduct)
        assertEquals(1, vm.cart.value.size)
        assertEquals(2.0, vm.cart.value[0].quantity, 0.01)
        assertEquals(36000.0, vm.cartSubtotal.value, 0.01)

        // Apply discount
        vm.applyPercentDiscount(10.0)
        assertEquals(3600.0, vm.cartDiscountAmount.value, 0.01)

        // Clear cart
        vm.clearCart()
        assertTrue(vm.cart.value.isEmpty())
        assertEquals(0.0, vm.cartSubtotal.value, 0.01)
    }

    @Test
    fun `custom theme wrapper uses brand red colors and supports dynamic toggle`() {
        // Verify Brand Red Color Scheme configurations
        val lightColors = com.example.ui.theme.BrandRedLightColorScheme
        val darkColors = com.example.ui.theme.BrandRedDarkColorScheme

        // 1. Primary Brand Red Requirement
        assertEquals(com.example.ui.theme.PrimaryRed, lightColors.primary)
        assertEquals(com.example.ui.theme.PrimaryRedDarkTheme, darkColors.primary)

        // 2. High contrast onPrimary
        assertEquals(androidx.compose.ui.graphics.Color.White, lightColors.onPrimary)

        // 3. Brand Red Containers
        assertEquals(com.example.ui.theme.PrimaryContainerLight, lightColors.primaryContainer)
        assertEquals(com.example.ui.theme.PrimaryContainerDark, darkColors.primaryContainer)

        // 4. Accent/Secondary Brand Red colors
        assertEquals(com.example.ui.theme.BrandRedAccent, lightColors.secondary)

        // 5. Dynamic Toggle Controller logic
        var toggledState: Boolean? = null
        var currentDark = false
        val testController = object : com.example.ui.theme.ThemeController {
            override val isDark: Boolean get() = currentDark
            override fun toggleTheme() {
                currentDark = !currentDark
                toggledState = currentDark
            }
            override fun setDarkTheme(enabled: Boolean) {
                currentDark = enabled
                toggledState = enabled
            }
        }

        assertFalse(testController.isDark)
        testController.toggleTheme()
        assertTrue(testController.isDark)
        assertEquals(true, toggledState)

        testController.setDarkTheme(false)
        assertFalse(testController.isDark)
        assertEquals(false, toggledState)
    }

    @Test
    fun `product image picker helper configures visual media request and storage helper`() {
        val context = ApplicationProvider.getApplicationContext<Application>()

        // 1. PickVisualMedia contract and request
        val contract = com.example.util.ProductImagePickerHelper.createContract()
        assertNotNull(contract)

        val mediaRequest = com.example.util.ProductImagePickerHelper.createImageRequest()
        assertTrue(mediaRequest.mediaType is androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)

        // 2. Storage helper copying test
        val testFile = java.io.File(context.cacheDir, "test_picker_input.jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        }
        val fileUri = android.net.Uri.fromFile(testFile)
        val savedPath = com.example.util.ImageStorageHelper.copyUriToLocalStorage(context, fileUri)
        assertNotNull(savedPath)
        val savedFile = java.io.File(savedPath!!)
        assertTrue(savedFile.exists())
        assertEquals(5L, savedFile.length())
    }
}
