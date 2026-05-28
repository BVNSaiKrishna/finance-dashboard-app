package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val amount: Double,
    val isExpense: Boolean,
    val date: String, // format YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
