package com.example.feature.confirmadas.domain.repository

import com.example.core.database.dao.AulaWithDetails
import com.example.core.database.entity.Aula
import com.example.core.database.entity.AulaFoto
import com.example.core.database.entity.EventoLog
import kotlinx.coroutines.flow.Flow

interface ConfirmadasRepository {
    fun getAulasWithDetailsFlow(): Flow<List<AulaWithDetails>>
    suspend fun getAulaWithDetailsById(aulaId: Long): AulaWithDetails?
    suspend fun getFotosForAula(aulaId: Long): List<AulaFoto>
    suspend fun getAulaById(aulaId: Long): Aula?
    suspend fun updateAula(aula: Aula)
    fun getAllLogsFlow(): Flow<List<EventoLog>>
    suspend fun insertEventoLog(log: EventoLog): Long
}
