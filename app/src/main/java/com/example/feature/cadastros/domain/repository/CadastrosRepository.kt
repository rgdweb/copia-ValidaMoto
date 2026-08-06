package com.example.feature.cadastros.domain.repository

import com.example.core.database.entity.Aluno
import com.example.core.database.entity.Moto
import com.example.core.database.dao.AulaWithDetails
import kotlinx.coroutines.flow.Flow

interface CadastrosRepository {
    fun getAlunosFlow(): Flow<List<Aluno>>
    fun getMotosFlow(): Flow<List<Moto>>
    fun getAulasWithDetailsFlow(): Flow<List<AulaWithDetails>>
    suspend fun insertAluno(aluno: Aluno): Long
    suspend fun updateAluno(aluno: Aluno)
    suspend fun deleteAluno(aluno: Aluno)
    suspend fun countActiveAulasByAluno(alunoId: Long): Int
    suspend fun countAulasByAluno(alunoId: Long): Int
    suspend fun countAgendamentosByAluno(alunoId: Long): Int
    suspend fun insertMoto(moto: Moto): Long
    suspend fun updateMoto(moto: Moto)
    suspend fun deleteMoto(moto: Moto)
}
