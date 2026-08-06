package com.example.feature.confirmadas.domain.usecases

import com.example.core.database.dao.AulaWithDetails
import com.example.feature.confirmadas.domain.repository.ConfirmadasRepository
import kotlinx.coroutines.flow.Flow

class GetAulasWithDetailsUseCase(private val repository: ConfirmadasRepository) {
    operator fun invoke(): Flow<List<AulaWithDetails>> = repository.getAulasWithDetailsFlow()
}
