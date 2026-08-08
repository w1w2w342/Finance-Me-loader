package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {
    val allExpenses: Flow<List<Expense>> = db.expenseDao().getAllExpenses()
    val budget: Flow<Budget?> = db.budgetDao().getBudget()

    suspend fun insertExpense(name: String, amount: Double, originalAmount: Double = amount, currency: String = "UAH", category: String) {
        db.expenseDao().insertExpense(Expense(name = name, amount = amount, originalAmount = originalAmount, currency = currency, category = category))
    }

    suspend fun deleteExpenseById(id: Int) {
        db.expenseDao().deleteExpenseById(id)
    }

    suspend fun updateBudget(amount: Double) {
        db.budgetDao().insertBudget(Budget(amount = amount))
    }
}
