package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cash_shifts",
    indices = [Index(value = ["openedAt"]), Index(value = ["isOpen"])]
)
data class CashShift(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val staffName: String,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val initialCash: Double, // Modal awal e.g. Rp 200.000
    val totalPayIn: Double = 0.0,
    val totalPayOut: Double = 0.0,
    val totalCashSales: Double = 0.0,
    val expectedCash: Double = 0.0,
    val actualCash: Double? = null,
    val difference: Double = 0.0,
    val isOpen: Boolean = true,
    val notes: String = ""
)

@Entity(
    tableName = "cash_entries",
    indices = [Index(value = ["cashShiftId"]), Index(value = ["timestamp"])]
)
data class CashEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cashShiftId: Long,
    val entryType: String, // PAY_IN, PAY_OUT
    val amount: Double,
    val category: String = "Operasional",
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val staffName: String = "Admin"
)
