package com.personal.expensetracker.data.repository

import com.personal.expensetracker.data.db.ExpenseDao
import com.personal.expensetracker.data.model.Expense
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val dao: ExpenseDao
) {
    fun getAllExpenses(): Flow<List<Expense>> = dao.getAllExpenses()

    fun getRecentExpenses(): Flow<List<Expense>> = dao.getRecentExpenses()

    fun getExpensesByDateRange(startTime: Long, endTime: Long): Flow<List<Expense>> =
        dao.getExpensesByDateRange(startTime, endTime)

    fun getExpensesFrom(startTime: Long): Flow<List<Expense>> =
        dao.getExpensesFrom(startTime)

    fun getTotalForPeriod(startTime: Long, endTime: Long): Flow<Double?> =
        dao.getTotalForPeriod(startTime, endTime)

    suspend fun insertExpense(expense: Expense): Long = dao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) = dao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = dao.deleteExpense(expense)

    suspend fun isDuplicateSms(rawSms: String): Boolean =
        dao.countBySmsContent(rawSms) > 0
}