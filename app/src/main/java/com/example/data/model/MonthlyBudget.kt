package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_budgets")
data class MonthlyBudget(
    @PrimaryKey val month: String, // format YYYY-MM
    val limit: Double
)
