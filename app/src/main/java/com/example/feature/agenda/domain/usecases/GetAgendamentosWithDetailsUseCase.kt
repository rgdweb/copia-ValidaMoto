package com.example.feature.agenda.domain.usecases

import com.example.core.database.dao.AgendamentoWithDetails
import com.example.feature.agenda.domain.repository.AgendaRepository
import kotlinx.coroutines.flow.Flow

class GetAgendamentosWithDetailsUseCase(private val repository: AgendaRepository) {
    operator fun invoke(): Flow<List<AgendamentoWithDetails>> {
        return repository.getAgendamentosWithDetailsFlow()
    }
}
