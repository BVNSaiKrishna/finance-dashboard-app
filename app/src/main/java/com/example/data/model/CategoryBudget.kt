package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_budgets")
data class CategoryBudget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val month: String, // format YYYY-MM
    val category: String,
    val limit: Double
)
