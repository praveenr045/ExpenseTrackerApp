package com.personal.expensetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.personal.expensetracker.data.model.Expense
import com.personal.expensetracker.data.model.ExpenseCategory
import com.personal.expensetracker.data.model.ExpenseSource

class Converters {
    @TypeConverter
    fun fromCategory(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): ExpenseCategory =
        ExpenseCategory.valueOf(value)

    @TypeConverter
    fun fromSource(value: ExpenseSource): String = value.name

    @TypeConverter
    fun toSource(value: String): ExpenseSource =
        ExpenseSource.valueOf(value)
}

@Database(entities = [Expense::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
}