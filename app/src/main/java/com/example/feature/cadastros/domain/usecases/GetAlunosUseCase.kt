package com.example.feature.cadastros.domain.usecases

import com.example.core.database.entity.Aluno
import com.example.feature.cadastros.domain.repository.CadastrosRepository
import kotlinx.coroutines.flow.Flow

class GetAlunosUseCase(private val repository: CadastrosRepository) {
    operator fun invoke(): Flow<List<Aluno>> = repository.getAlunosFlow()
}
