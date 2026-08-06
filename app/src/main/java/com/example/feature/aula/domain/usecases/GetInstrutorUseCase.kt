package com.example.feature.aula.domain.usecases

import com.example.core.database.entity.Instrutor
import com.example.feature.aula.domain.repository.AulaRepository
import kotlinx.coroutines.flow.Flow

class GetInstrutorUseCase(private val repository: AulaRepository) {
    operator fun invoke(): Flow<Instrutor?> = repository.getInstrutorFlow()
    suspend fun getDirect(): Instrutor? = repository.getInstrutor()
}
