package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["transactionNo"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["customerId"]),
        Index(value = ["kitchenStatus"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionNo: String,
    val timestamp: Long = System.currentTimeMillis(),
    val customerId: Long? = null,
    val customerName: String = "Pelanggan Umum",
    val customerPhone: String = "",
    val cashierName: String = "Kasir",
    val subtotal: Double,
    val discountAmount: Double = 0.0,
    val discountNote: String = "",
    val taxAmount: Double = 0.0,
    val taxRate: Double = 0.0,
    val serviceCharge: Double = 0.0,
    val total: Double,
    val paymentStatus: String = "PAID", // PAID, REFUNDED, VOID
    val paymentMethodsSummary: String = "", // e.g. "Tunai Rp50.000, GoPay Rp50.000"
    val changeAmount: Double = 0.0,
    val notes: String = "",
    val tableNo: String = "",
    val kitchenStatus: String = "BARU", // BARU, DIPROSES, SIAP, SELESAI
    val kitchenUpdatedTime: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transaction_items",
    indices = [Index(value = ["transactionId"]), Index(value = ["productId"])]
)
data class TransactionItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long = 0L,
    val productId: Long,
    val productName: String,
    val price: Double,
    val costPrice: Double = 0.0,
    val quantity: Double,
    val unit: String = "pcs",
    val variantNote: String = "",
    val itemDiscount: Double = 0.0,
    val total: Double
)

@Entity(
    tableName = "transaction_payments",
    indices = [Index(value = ["transactionId"])]
)
data class TransactionPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long = 0L,
    val paymentMethod: String,
    val amount: Double,
    val referenceNo: String = ""
)
