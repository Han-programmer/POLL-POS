package com.example.data.dao

import androidx.room.*
import com.example.data.model.Transaction
import com.example.data.model.TransactionItem
import com.example.data.model.TransactionPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE transactionNo = :transactionNo LIMIT 1")
    suspend fun getTransactionByNo(transactionNo: String): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("UPDATE transactions SET paymentStatus = :status WHERE id = :id")
    suspend fun updateTransactionStatus(id: Long, status: String)

    // Items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItem>)

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItem>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    fun getItemsForTransactionFlow(transactionId: Long): Flow<List<TransactionItem>>

    @Query("SELECT * FROM transaction_items")
    suspend fun getAllTransactionItems(): List<TransactionItem>

    // Payments
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionPayments(payments: List<TransactionPayment>)

    @Query("SELECT * FROM transaction_payments WHERE transactionId = :transactionId")
    suspend fun getPaymentsForTransaction(transactionId: Long): List<TransactionPayment>

    @Query("SELECT * FROM transaction_payments")
    suspend fun getAllTransactionPayments(): List<TransactionPayment>

    // Kitchen Orders
    @Query("SELECT * FROM transactions WHERE kitchenStatus IN ('BARU', 'DIPROSES', 'SIAP') ORDER BY timestamp ASC")
    fun getActiveKitchenOrders(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE kitchenStatus = :status ORDER BY timestamp ASC")
    fun getKitchenOrdersByStatus(status: String): Flow<List<Transaction>>

    @Query("UPDATE transactions SET kitchenStatus = :status, kitchenUpdatedTime = :time WHERE id = :id")
    suspend fun updateKitchenStatus(id: Long, status: String, time: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countTransactions(): Int
}
