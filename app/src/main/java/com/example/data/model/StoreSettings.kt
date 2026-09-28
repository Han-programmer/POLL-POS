package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_settings")
data class StoreSettings(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "POLL Cafe & Resto",
    val address: String = "Jl. Merdeka No. 45, Jakarta",
    val phone: String = "0812-3456-7890",
    val taxRate: Double = 10.0,
    val enableTax: Boolean = false,
    val serviceChargeRate: Double = 5.0,
    val enableServiceCharge: Boolean = false,
    val paperSize: String = "58mm", // 58mm, 80mm
    val receiptHeader: String = "POLL POS\nSolusi Usaha Indonesia",
    val receiptFooter: String = "Terima kasih atas kunjungan Anda!\nFollow IG: @pollpos.id",
    val bluetoothPrinterAddress: String? = null,
    val bluetoothPrinterName: String? = null,
    val currencySymbol: String = "Rp",
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val logoUri: String? = null, // Logo profil toko
    val showLogoOnReceipt: Boolean = true, // Tampilkan logo di struk
    val customReceiptStoreName: String = "", // Custom nama toko di struk (opsional)
    val receiptCustomLogoUri: String? = null // Logo custom khusus struk (opsional)
)
