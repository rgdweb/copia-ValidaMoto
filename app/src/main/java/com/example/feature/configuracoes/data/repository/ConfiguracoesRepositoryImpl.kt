package com.example.feature.configuracoes.data.repository

import com.example.core.database.AppDatabase
import com.example.core.database.dao.AulaWithDetails
import com.example.core.database.entity.Aluno
import com.example.core.database.entity.EventoLog
import com.example.core.database.entity.Instrutor
import com.example.feature.configuracoes.domain.repository.ConfiguracoesRepository
import kotlinx.coroutines.flow.Flow

class ConfiguracoesRepositoryImpl(private val db: AppDatabase) : ConfiguracoesRepository {
    private val instrutorDao = db.instrutorDao()
    private val alunoDao = db.alunoDao()
    private val aulaDao = db.aulaDao()
    private val eventoLogDao = db.eventoLogDao()

    override fun getInstrutorFlow(): Flow<Instrutor?> = instrutorDao.getInstrutorFlow()
    override fun getAlunosFlow(): Flow<List<Aluno>> = alunoDao.getAlunosFlow()
    override fun getAulasWithDetailsFlow(): Flow<List<AulaWithDetails>> = aulaDao.getAulasWithDetailsFlow()
    
    override suspend fun saveInstrutor(instrutor: Instrutor): Long {
        val existing = instrutorDao.getInstrutor()
        return if (existing != null) {
            val updated = instrutor.copy(id = existing.id)
            instrutorDao.update(updated)
            existing.id
        } else {
            instrutorDao.insert(instrutor)
        }
    }
    
    override suspend fun insertEventoLog(log: EventoLog): Long = eventoLogDao.insert(log)
}
