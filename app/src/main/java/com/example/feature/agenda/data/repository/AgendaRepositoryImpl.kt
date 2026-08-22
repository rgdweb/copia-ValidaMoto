package com.example.feature.agenda.data.repository
import com.example.core.database.AppDatabase
import com.example.core.database.entity.Agendamento
import com.example.core.database.entity.Aluno
import com.example.core.database.entity.Moto
import com.example.core.database.entity.EventoLog
import com.example.core.database.dao.AgendamentoWithDetails
import com.example.feature.agenda.domain.repository.AgendaRepository
import kotlinx.coroutines.flow.Flow
class AgendaRepositoryImpl(private val db: AppDatabase) : AgendaRepository {
    private val agDao = db.agendamentoDao(); private val alDao = db.alunoDao(); private val moDao = db.motoDao(); private val elDao = db.eventoLogDao()
    override fun getAgendamentosWithDetailsFlow() = agDao.getAgendamentosWithDetailsFlow()
    override fun getAlunosFlow() = alDao.getAlunosFlow()
    override fun getMotosFlow() = moDao.getMotosFlow()
    override suspend fun insertAgendamento(a: Agendamento): Long = agDao.insert(a)
    override suspend fun updateAgendamento(a: Agendamento) { agDao.update(a) }
    override suspend fun deleteAgendamento(a: Agendamento) { agDao.delete(a) }
    override suspend fun deleteAgendamentoById(id: Long) { agDao.deleteById(id) }
    override suspend fun auditLog(t: String, d: String) { elDao.insert(EventoLog(timestamp = System.currentTimeMillis(), tipo = t, usuario = "Instrutor", alunoId = null, alunoNome = null, instrutorId = null, instrutorNome = null, motoId = null, motoModelo = null, descricao = d)) }
    override suspend fun getExameAgendamentoByAlunoId(id: Long) = agDao.getExameAgendamentoByAlunoId(id)
}
