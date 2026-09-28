package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun getAllStockMovements(): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC")
    fun getMovementsForProduct(productId: Long): Flow<List<StockMovement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movements: List<StockMovement>)
}

@Dao
interface CashShiftDao {
    @Query("SELECT * FROM cash_shifts WHERE isOpen = 1 ORDER BY openedAt DESC LIMIT 1")
    fun getCurrentOpenShift(): Flow<CashShift?>

    @Query("SELECT * FROM cash_shifts WHERE isOpen = 1 ORDER BY openedAt DESC LIMIT 1")
    suspend fun getCurrentOpenShiftDirect(): CashShift?

    @Query("SELECT * FROM cash_shifts WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: Long): CashShift?

    @Query("SELECT * FROM cash_shifts ORDER BY openedAt DESC")
    fun getAllShifts(): Flow<List<CashShift>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: CashShift): Long

    @Update
    suspend fun updateShift(shift: CashShift)

    // Cash entries (Pay In / Pay Out)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashEntry(entry: CashEntry): Long

    @Query("SELECT * FROM cash_entries WHERE cashShiftId = :shiftId ORDER BY timestamp DESC")
    fun getEntriesForShift(shiftId: Long): Flow<List<CashEntry>>

    @Query("SELECT * FROM cash_entries ORDER BY timestamp DESC")
    fun getAllCashEntries(): Flow<List<CashEntry>>
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff_users WHERE isActive = 1 ORDER BY name ASC")
    fun getAllStaff(): Flow<List<StaffUser>>

    @Query("SELECT * FROM staff_users WHERE pin = :pin AND isActive = 1 LIMIT 1")
    suspend fun getStaffByPin(pin: String): StaffUser?

    @Query("SELECT * FROM staff_users WHERE id = :id LIMIT 1")
    suspend fun getStaffById(id: Long): StaffUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffUser): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(staffList: List<StaffUser>)

    @Update
    suspend fun updateStaff(staff: StaffUser)

    @Delete
    suspend fun deleteStaff(staff: StaffUser)

    @Query("SELECT COUNT(*) FROM staff_users")
    suspend fun countStaff(): Int
}

@Dao
interface StoreSettingsDao {
    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<StoreSettings?>

    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): StoreSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: StoreSettings)
}
