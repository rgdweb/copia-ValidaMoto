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
    private val agendamentoDao = db.agendamentoDao()
    private val alunoDao = db.alunoDao()
    private val motoDao = db.motoDao()
    private val eventoLogDao = db.eventoLogDao()

    override fun getAgendamentosWithDetailsFlow(): Flow<List<AgendamentoWithDetails>> {
        return agendamentoDao.getAgendamentosWithDetailsFlow()
    }

    override fun getAlunosFlow(): Flow<List<Aluno>> {
        return alunoDao.getAlunosFlow()
    }

    override fun getMotosFlow(): Flow<List<Moto>> {
        return motoDao.getMotosFlow()
    }

    override suspend fun insertAgendamento(agendamento: Agendamento): Long {
        return agendamentoDao.insert(agendamento)
    }

    override suspend fun updateAgendamento(agendamento: Agendamento) {
        agendamentoDao.update(agendamento)
    }

    override suspend fun deleteAgendamento(agendamento: Agendamento) {
        agendamentoDao.delete(agendamento)
    }

    override suspend fun auditLog(tipo: String, descricao: String) {
        val log = EventoLog(
            timestamp = System.currentTimeMillis(),
            tipo = tipo,
            usuario = "Instrutor",
            alunoId = null,
            alunoNome = null,
            instrutorId = null,
            instrutorNome = null,
            motoId = null,
            motoModelo = null,
            descricao = descricao
        )
        eventoLogDao.insert(log)
    }
}
