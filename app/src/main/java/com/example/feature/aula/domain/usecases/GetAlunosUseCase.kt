package com.example.feature.aula.domain.usecases

import com.example.core.database.entity.Aluno
import com.example.feature.aula.domain.repository.AulaRepository
import kotlinx.coroutines.flow.Flow

class GetAlunosUseCase(private val repository: AulaRepository) {
    operator fun invoke(): Flow<List<Aluno>> = repository.getAlunosFlow()
}
