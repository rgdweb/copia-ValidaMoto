package com.example.core.database.dao

import androidx.room.*
import com.example.core.database.entity.Instrutor
import kotlinx.coroutines.flow.Flow

@Dao
interface InstrutorDao {
    @Query("SELECT * FROM instrutor LIMIT 1")
    fun getInstrutorFlow(): Flow<Instrutor?>

    @Query("SELECT * FROM instrutor LIMIT 1")
    suspend fun getInstrutor(): Instrutor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(instrutor: Instrutor): Long

    @Update
    suspend fun update(instrutor: Instrutor)
}
