package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["productId"]), Index(value = ["timestamp"])]
)
data class StockMovement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val movementType: String, // IN, OUT, ADJUSTMENT, SALE, REFUND, OPNAME
    val quantity: Double,
    val previousStock: Double,
    val newStock: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val staffName: String = "Admin"
)
