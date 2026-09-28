package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["categoryId"]), Index(value = ["sku"]), Index(value = ["barcode"])]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val imageUri: String? = null,
    val price: Double,
    val costPrice: Double = 0.0,
    val stock: Double = 0.0,
    val minStock: Double = 5.0,
    val sku: String = "",
    val barcode: String = "",
    val categoryId: Long = 0,
    val unit: String = "pcs",
    val description: String = "",
    val variantsJson: String = "",
    val isActive: Boolean = true
)

data class ProductVariantGroup(
    val name: String, // e.g. "Ukuran", "Level Pedas", "Suhu"
    val options: List<ProductVariantOption>
)

data class ProductVariantOption(
    val name: String, // e.g. "Besar", "Pedas Sedang", "Dingin"
    val extraPrice: Double = 0.0
)
