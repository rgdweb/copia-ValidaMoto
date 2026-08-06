package com.example.feature.aula.domain.usecases

import com.example.core.database.entity.Moto
import com.example.feature.aula.domain.repository.AulaRepository
import kotlinx.coroutines.flow.Flow

class GetMotosUseCase(private val repository: AulaRepository) {
    operator fun invoke(): Flow<List<Moto>> = repository.getMotosFlow()
}
