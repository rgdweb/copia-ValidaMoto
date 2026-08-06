package com.example.feature.cadastros.domain.usecases

import com.example.core.database.entity.Moto
import com.example.feature.cadastros.domain.repository.CadastrosRepository
import kotlinx.coroutines.flow.Flow

class GetMotosUseCase(private val repository: CadastrosRepository) {
    operator fun invoke(): Flow<List<Moto>> = repository.getMotosFlow()
}
