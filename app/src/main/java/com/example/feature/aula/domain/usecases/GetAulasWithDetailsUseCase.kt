package com.example.feature.aula.domain.usecases

import com.example.core.database.dao.AulaWithDetails
import com.example.feature.aula.domain.repository.AulaRepository
import kotlinx.coroutines.flow.Flow

class GetAulasWithDetailsUseCase(private val repository: AulaRepository) {
    operator fun invoke(): Flow<List<AulaWithDetails>> = repository.getAulasWithDetailsFlow()
}
