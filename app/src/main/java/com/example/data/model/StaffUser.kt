package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staff_users",
    indices = [Index(value = ["pin"])]
)
data class StaffUser(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val role: String, // OWNER, ADMIN, MANAGER, KASIR, KITCHEN
    val pin: String, // e.g. "1234"
    val canDiscount: Boolean = true,
    val canRefund: Boolean = true,
    val canVoid: Boolean = true,
    val canChangePrice: Boolean = true,
    val canManageStock: Boolean = true,
    val canViewReports: Boolean = true,
    val canManageSettings: Boolean = true,
    val isActive: Boolean = true
)
