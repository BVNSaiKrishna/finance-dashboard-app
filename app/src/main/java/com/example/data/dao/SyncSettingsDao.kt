package com.example.data.dao

import androidx.room.*
import com.example.data.model.SyncSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncSettingsDao {
    @Query("SELECT * FROM sync_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<SyncSettings?>

    @Query("SELECT * FROM sync_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): SyncSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: SyncSettings)
}
