package com.example.feature.confirmadas.data.repository

import com.example.core.database.AppDatabase
import com.example.core.database.dao.AulaWithDetails
import com.example.core.database.entity.Aula
import com.example.core.database.entity.AulaFoto
import com.example.core.database.entity.EventoLog
import com.example.feature.confirmadas.domain.repository.ConfirmadasRepository
import kotlinx.coroutines.flow.Flow

class ConfirmadasRepositoryImpl(private val db: AppDatabase) : ConfirmadasRepository {
    private val aulaDao = db.aulaDao()
    private val aulaFotoDao = db.aulaFotoDao()
    private val eventoLogDao = db.eventoLogDao()

    override fun getAulasWithDetailsFlow(): Flow<List<AulaWithDetails>> = aulaDao.getAulasWithDetailsFlow()
    override suspend fun getAulaWithDetailsById(aulaId: Long): AulaWithDetails? = aulaDao.getAulaWithDetailsById(aulaId)
    override suspend fun getFotosForAula(aulaId: Long): List<AulaFoto> = aulaFotoDao.getFotosForAula(aulaId)
    override suspend fun getAulaById(aulaId: Long): Aula? = aulaDao.getAulaById(aulaId)
    override suspend fun updateAula(aula: Aula) = aulaDao.update(aula)
    override fun getAllLogsFlow(): Flow<List<EventoLog>> = eventoLogDao.getAllLogsFlow()
    override suspend fun insertEventoLog(log: EventoLog): Long = eventoLogDao.insert(log)
}
