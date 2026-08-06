package com.example.feature.configuracoes.domain.repository

import com.example.core.database.dao.AulaWithDetails
import com.example.core.database.entity.Aluno
import com.example.core.database.entity.EventoLog
import com.example.core.database.entity.Instrutor
import kotlinx.coroutines.flow.Flow

interface ConfiguracoesRepository {
    fun getInstrutorFlow(): Flow<Instrutor?>
    fun getAlunosFlow(): Flow<List<Aluno>>
    fun getAulasWithDetailsFlow(): Flow<List<AulaWithDetails>>
    suspend fun saveInstrutor(instrutor: Instrutor): Long
    suspend fun insertEventoLog(log: EventoLog): Long
}
