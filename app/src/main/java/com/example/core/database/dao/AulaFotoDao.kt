package com.example.core.database.dao

import androidx.room.*
import com.example.core.database.entity.AulaFoto

@Dao
interface AulaFotoDao {
    @Query("SELECT * FROM aula_foto WHERE aulaId = :aulaId ORDER BY timestamp ASC")
    suspend fun getFotosForAula(aulaId: Long): List<AulaFoto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(aulaFoto: AulaFoto): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(aulaFotos: List<AulaFoto>)
}
