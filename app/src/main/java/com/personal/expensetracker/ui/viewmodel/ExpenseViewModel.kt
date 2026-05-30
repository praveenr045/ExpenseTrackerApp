package com.personal.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.expensetracker.data.model.Expense
import com.personal.expensetracker.data.model.ExpenseCategory
import com.personal.expensetracker.data.model.ExpenseSource
import com.personal.expensetracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _allExpenses = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allExpenses: StateFlow<List<Expense>> = _allExpenses

    val recentExpenses: StateFlow<List<Expense>> = repository.getRecentExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactive monthly total derived from allExpenses — updates immediately on insert
    val monthlyTotal: StateFlow<Double> = _allExpenses.map { list ->
        val start = startOfMonth()
        list.filter { it.timestamp >= start }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Today's total
    val dailyTotal: StateFlow<Double> = _allExpenses.map { list ->
        val start = startOfDay()
        list.filter { it.timestamp >= start }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categoryBreakdown: StateFlow<Map<ExpenseCategory, Double>> = _allExpenses.map { list ->
        val start = startOfMonth()
        list.filter { it.timestamp >= start }
            .groupBy { it.category }
            .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val dailySpending: StateFlow<Map<Int, Double>> = _allExpenses.map { list ->
        val start = startOfMonth()
        list.filter { it.timestamp >= start }
            .groupBy { dayOfMonth(it.timestamp) }
            .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun getExpensesByRange(startTime: Long, endTime: Long): Flow<List<Expense>> =
        repository.getExpensesByDateRange(startTime, endTime)

    fun addExpense(
        amount: Double,
        description: String,
        merchant: String = "",
        category: ExpenseCategory = ExpenseCategory.OTHER,
        source: ExpenseSource = ExpenseSource.MANUAL,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.insertExpense(
                Expense(
                    amount = amount,
                    description = description,
                    merchant = merchant,
                    category = category,
                    source = source,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch { repository.updateExpense(expense) }
    }

    fun startOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun startOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun dayOfMonth(timestamp: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.DAY_OF_MONTH)
    }
}