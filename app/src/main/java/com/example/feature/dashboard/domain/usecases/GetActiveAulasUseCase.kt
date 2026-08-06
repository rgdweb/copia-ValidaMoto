package com.example.feature.dashboard.domain.usecases

import com.example.core.database.dao.AulaWithDetails
import com.example.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetActiveAulasUseCase(private val repository: DashboardRepository) {
    operator fun invoke(): Flow<List<AulaWithDetails>> {
        return repository.getAulasWithDetailsFlow().map { list ->
            // Filter active lessons (lessons in progress)
            list.filter { it.statusAula == "pendente" && it.dataHoraInicio > 0L && it.dataHoraFim == 0L }
        }
    }
}
