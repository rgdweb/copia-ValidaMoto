package com.example.core.database.dao

import androidx.room.*
import com.example.core.database.entity.EventoLog
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoLogDao {
    @Query("SELECT * FROM evento_log ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<EventoLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: EventoLog): Long
}
