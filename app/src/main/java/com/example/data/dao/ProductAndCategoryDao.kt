package com.example.data.dao

import androidx.room.*
import com.example.data.model.Category
import com.example.data.model.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND isActive = 1 ORDER BY name ASC")
    fun getProductsByCategory(categoryId: Long): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE stock <= minStock AND isActive = 1 ORDER BY stock ASC")
    fun getLowStockProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode OR sku = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET stock = :newStock WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Double)

    @Query("UPDATE products SET stock = stock - :qty WHERE id = :productId")
    suspend fun deductStock(productId: Long, qty: Double)

    @Query("UPDATE products SET stock = stock + :qty WHERE id = :productId")
    suspend fun addStock(productId: Long, qty: Double)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Query("SELECT * FROM products")
    suspend fun getAllProductsSync(): List<Product>
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int
}
