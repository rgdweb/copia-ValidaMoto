package com.example.feature.dashboard.data.repository

import com.example.core.database.AppDatabase
import com.example.core.database.entity.*
import com.example.core.database.dao.AulaWithDetails
import com.example.core.preferences.AppPreferences
import com.example.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow

class DashboardRepositoryImpl(
    private val db: AppDatabase,
    private val prefs: AppPreferences
) : DashboardRepository {

    override fun getAulasWithDetailsFlow(): Flow<List<AulaWithDetails>> = db.aulaDao().getAulasWithDetailsFlow()
    override fun getAlunosFlow(): Flow<List<Aluno>> = db.alunoDao().getAlunosFlow()
    override fun getMotosFlow(): Flow<List<Moto>> = db.motoDao().getMotosFlow()
    override fun getInstrutorFlow(): Flow<Instrutor?> = db.instrutorDao().getInstrutorFlow()

    override fun getGoogleLastSyncTime(): String = prefs.googleLastSyncTime ?: "Nunca"
    override fun isGoogleBackupEnabled(): Boolean = prefs.isGoogleBackupEnabled
    override fun getGoogleAccountEmail(): String? = prefs.googleAccountEmail

    override suspend fun updateAula(aula: Aula) {
        db.aulaDao().update(aula)
    }

    override suspend fun getAulaById(id: Long): Aula? = db.aulaDao().getAulaById(id)
}
