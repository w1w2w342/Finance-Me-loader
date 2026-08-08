package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val originalAmount: Double = amount,
    val currency: String = "UAH",
    val category: String = "Other",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "budget")
data class Budget(
    @PrimaryKey val id: Int = 1,
    val amount: Double
)
