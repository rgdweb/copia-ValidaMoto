package com.example.feature.cadastros.domain.repository
import com.example.core.database.entity.Agendamento
import com.example.core.database.entity.Aluno
import com.example.core.database.entity.Moto
import com.example.core.database.dao.AulaWithDetails
import kotlinx.coroutines.flow.Flow
interface CadastrosRepository {
    fun getAlunosFlow(): Flow<List<Aluno>>
    fun getMotosFlow(): Flow<List<Moto>>
    fun getAulasWithDetailsFlow(): Flow<List<AulaWithDetails>>
    suspend fun insertAluno(a: Aluno): Long
    suspend fun updateAluno(a: Aluno)
    suspend fun deleteAluno(a: Aluno)
    suspend fun countActiveAulasByAluno(id: Long): Int
    suspend fun countAulasByAluno(id: Long): Int
    suspend fun countAgendamentosByAluno(id: Long): Int
    suspend fun insertMoto(m: Moto): Long
    suspend fun updateMoto(m: Moto)
    suspend fun deleteMoto(m: Moto)
    suspend fun getExameAgendamentoByAlunoId(id: Long): Agendamento?
    suspend fun deleteAgendamentoById(id: Long)
}
