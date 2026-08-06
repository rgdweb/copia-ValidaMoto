package com.example.feature.aula.domain.repository

import com.example.core.database.entity.*
import com.example.core.database.dao.AulaWithDetails
import kotlinx.coroutines.flow.Flow

interface AulaRepository {
    fun getAulasWithDetailsFlow(): Flow<List<AulaWithDetails>>
    fun getAlunosFlow(): Flow<List<Aluno>>
    fun getMotosFlow(): Flow<List<Moto>>
    fun getInstrutorFlow(): Flow<Instrutor?>
    fun getAllLogsFlow(): Flow<List<EventoLog>>

    suspend fun getInstrutor(): Instrutor?
    suspend fun getAlunoById(id: Long): Aluno?
    suspend fun getMotoById(id: Long): Moto?
    suspend fun getAulaWithDetailsById(id: Long): AulaWithDetails?
    suspend fun getAulaById(id: Long): Aula?
    suspend fun getCountAulasConfirmadas(alunoId: Long): Int
    suspend fun getFotosForAula(aulaId: Long): List<AulaFoto>

    suspend fun insertAula(aula: Aula): Long
    suspend fun updateAula(aula: Aula)
    suspend fun deleteAula(aula: Aula)
    suspend fun updateAluno(aluno: Aluno)
    suspend fun updateMoto(moto: Moto)
    suspend fun insertAulaFoto(aulaFoto: AulaFoto): Long
    suspend fun insertAllAulaFotos(aulaFotos: List<AulaFoto>)
    suspend fun insertEventoLog(log: EventoLog): Long
    suspend fun saveInstrutor(instrutor: Instrutor): Long

    suspend fun <R> runInTransaction(block: suspend () -> R): R
}
