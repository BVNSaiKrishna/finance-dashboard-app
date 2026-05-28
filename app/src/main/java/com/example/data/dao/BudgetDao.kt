package com.example.data.dao

import androidx.room.*
import com.example.data.model.CategoryBudget
import com.example.data.model.MonthlyBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE month = :month LIMIT 1")
    fun getMonthlyBudget(month: String): Flow<MonthlyBudget?>

    @Query("SELECT * FROM monthly_budgets")
    fun getAllMonthlyBudgets(): Flow<List<MonthlyBudget>>

    @Query("SELECT * FROM category_budgets WHERE month = :month")
    fun getCategoryBudgets(month: String): Flow<List<CategoryBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyBudget(budget: MonthlyBudget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryBudget(categoryBudget: CategoryBudget)

    @Query("DELETE FROM category_budgets WHERE id = :id")
    suspend fun deleteCategoryBudget(id: Int)

    @Query("DELETE FROM category_budgets WHERE month = :month")
    suspend fun clearCategoryBudgetsForMonth(month: String)
}
