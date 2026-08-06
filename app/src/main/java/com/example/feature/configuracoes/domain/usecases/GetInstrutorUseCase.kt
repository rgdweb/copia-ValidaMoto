package com.example.feature.configuracoes.domain.usecases

import com.example.core.database.entity.Instrutor
import com.example.feature.configuracoes.domain.repository.ConfiguracoesRepository
import kotlinx.coroutines.flow.Flow

class GetInstrutorUseCase(private val repository: ConfiguracoesRepository) {
    operator fun invoke(): Flow<Instrutor?> = repository.getInstrutorFlow()
}
