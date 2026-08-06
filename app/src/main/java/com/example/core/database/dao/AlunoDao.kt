package com.example.core.database.dao

import androidx.room.*
import com.example.core.database.entity.Aluno
import kotlinx.coroutines.flow.Flow

@Dao
interface AlunoDao {
    @Query("SELECT * FROM aluno ORDER BY nome ASC")
    fun getAlunosFlow(): Flow<List<Aluno>>

    @Query("SELECT * FROM aluno WHERE id = :id")
    suspend fun getAlunoById(id: Long): Aluno?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(aluno: Aluno): Long

    @Update
    suspend fun update(aluno: Aluno)

    @Delete
    suspend fun delete(aluno: Aluno)
}
