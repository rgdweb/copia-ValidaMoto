package com.example.feature.agenda.domain.usecases

import com.example.core.database.entity.Aluno
import com.example.feature.agenda.domain.repository.AgendaRepository
import kotlinx.coroutines.flow.Flow

class GetAlunosUseCase(private val repository: AgendaRepository) {
    operator fun invoke(): Flow<List<Aluno>> {
        return repository.getAlunosFlow()
    }
}
