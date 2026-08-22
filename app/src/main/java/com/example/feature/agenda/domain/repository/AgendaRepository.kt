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
    suspend fun insertAgendamento(a: Agendamento): Long
    suspend fun updateAgendamento(a: Agendamento)
    suspend fun deleteAgendamento(a: Agendamento)
    suspend fun deleteAgendamentoById(id: Long)
    suspend fun auditLog(tipo: String, desc: String)
    suspend fun getExameAgendamentoByAlunoId(alunoId: Long): Agendamento?
}
