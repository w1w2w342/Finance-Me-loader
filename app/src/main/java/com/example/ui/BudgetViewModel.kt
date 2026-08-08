package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.Expense
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudgetViewModel(private val repository: AppRepository) : ViewModel() {

    val expenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val budgetAmount: StateFlow<Double> = repository.budget
        .map { it?.amount ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    fun addExpense(name: String, amount: Double, category: String, currency: String = "UAH") {
        viewModelScope.launch {
            val rate = com.example.CurrencyService.getRateToUAH(currency)
            val convertedAmount = amount * rate
            repository.insertExpense(name = name, amount = convertedAmount, originalAmount = amount, currency = currency, category = category)
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            repository.deleteExpenseById(id)
        }
    }

    fun updateBudget(amount: Double) {
        viewModelScope.launch {
            repository.updateBudget(amount)
        }
    }
}

class BudgetViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
