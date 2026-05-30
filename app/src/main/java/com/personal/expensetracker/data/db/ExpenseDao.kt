package com.personal.expensetracker.data.db

import androidx.room.*
import com.personal.expensetracker.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getExpensesByDateRange(startTime: Long, endTime: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getExpensesFrom(startTime: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp >= :startTime AND timestamp <= :endTime")
    fun getTotalForPeriod(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT 5")
    fun getRecentExpenses(): Flow<List<Expense>>

    @Query("SELECT COUNT(*) FROM expenses WHERE rawSms = :rawSms AND rawSms != ''")
    suspend fun countBySmsContent(rawSms: String): Int
}