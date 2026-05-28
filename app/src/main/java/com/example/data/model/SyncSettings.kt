package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ConnectionStatus {
    CONNECTED,
    NOT_CONNECTED,
    ERROR,
    SYNCING
}

@Entity(tableName = "sync_settings")
data class SyncSettings(
    @PrimaryKey val id: Int = 1, // Only single configuration record allowed
    val spreadsheetUrl: String = "",
    val tabName: String = "Sheet1",
    val lastSyncedTime: Long = 0,
    val connectionStatus: ConnectionStatus = ConnectionStatus.NOT_CONNECTED,
    val errorMessage: String? = null
)
