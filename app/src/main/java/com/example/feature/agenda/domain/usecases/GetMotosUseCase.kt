package com.example.feature.agenda.domain.usecases

import com.example.core.database.entity.Moto
import com.example.feature.agenda.domain.repository.AgendaRepository
import kotlinx.coroutines.flow.Flow

class GetMotosUseCase(private val repository: AgendaRepository) {
    operator fun invoke(): Flow<List<Moto>> {
        return repository.getMotosFlow()
    }
}
