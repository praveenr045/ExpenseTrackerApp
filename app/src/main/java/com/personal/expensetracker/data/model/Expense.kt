package com.personal.expensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ExpenseCategory(val displayName: String, val emoji: String) {
    FOOD("Food & Dining", "🍽️"),
    TRANSPORT("Transport", "🚗"),
    SHOPPING("Shopping", "🛍️"),
    BILLS("Bills & Utilities", "💡"),
    HEALTH("Health", "💊"),
    ENTERTAINMENT("Entertainment", "🎬"),
    GROCERIES("Groceries", "🛒"),
    FUEL("Fuel", "⛽"),
    EDUCATION("Education", "📚"),
    OTHER("Other", "💰")
}

enum class ExpenseSource {
    SMS, VOICE, MANUAL
}

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val merchant: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val source: ExpenseSource = ExpenseSource.MANUAL,
    val timestamp: Long = System.currentTimeMillis(),
    val rawSms: String = ""
)