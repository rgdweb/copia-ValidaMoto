package com.example.feature.agenda.domain.repository

import com.example.core.database.entity.Agendamento
import com.example.core.database.entity.Aluno
import com.example.core.database.entity.Moto
import com.example.core.database.dao.AgendamentoWithDetails
import kotlinx.coroutines.flow.Flow

interface AgendaRepository {
    fun getAgendamentosWithDetailsFlow(): Flow<List<AgendamentoWithDetails>>
    fun getAlunosFlow(): Flow<List<Aluno>>
    fun getMotosFlow(): Flow<List<Moto>>
    suspend fun insertAgendamento(agendamento: Agendamento): Long
    suspend fun updateAgendamento(agendamento: Agendamento)
    suspend fun deleteAgendamento(agendamento: Agendamento)
    suspend fun getExameAgendamentoByAlunoId(alunoId: Long): Agendamento?
    suspend fun deleteAgendamentoById(id: Long)
    suspend fun auditLog(tipo: String, descricao: String)
}
