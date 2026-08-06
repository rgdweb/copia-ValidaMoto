package com.example.core.database.dao

import androidx.room.*
import com.example.core.database.entity.Moto
import kotlinx.coroutines.flow.Flow

@Dao
interface MotoDao {
    @Query("SELECT * FROM moto ORDER BY modelo ASC")
    fun getMotosFlow(): Flow<List<Moto>>

    @Query("SELECT * FROM moto WHERE id = :id")
    suspend fun getMotoById(id: Long): Moto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(moto: Moto): Long

    @Update
    suspend fun update(moto: Moto)

    @Delete
    suspend fun delete(moto: Moto)
}
