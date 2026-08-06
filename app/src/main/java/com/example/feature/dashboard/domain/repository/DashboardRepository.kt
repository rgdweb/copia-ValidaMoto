package com.example.feature.dashboard.domain.repository

import com.example.core.database.entity.*
import com.example.core.database.dao.AulaWithDetails
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getAulasWithDetailsFlow(): Flow<List<AulaWithDetails>>
    fun getAlunosFlow(): Flow<List<Aluno>>
    fun getMotosFlow(): Flow<List<Moto>>
    fun getInstrutorFlow(): Flow<Instrutor?>
    
    // Preferences values
    fun getGoogleLastSyncTime(): String
    fun isGoogleBackupEnabled(): Boolean
    fun getGoogleAccountEmail(): String?

    suspend fun updateAula(aula: Aula)
    suspend fun getAulaById(id: Long): Aula?
}
