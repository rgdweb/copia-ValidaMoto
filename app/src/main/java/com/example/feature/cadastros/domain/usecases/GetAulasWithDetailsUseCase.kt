package com.example.feature.cadastros.domain.usecases

import com.example.core.database.dao.AulaWithDetails
import com.example.feature.cadastros.domain.repository.CadastrosRepository
import kotlinx.coroutines.flow.Flow

class GetAulasWithDetailsUseCase(private val repository: CadastrosRepository) {
    operator fun invoke(): Flow<List<AulaWithDetails>> = repository.getAulasWithDetailsFlow()
}
