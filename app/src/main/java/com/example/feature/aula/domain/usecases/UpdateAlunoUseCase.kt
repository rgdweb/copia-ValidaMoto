package com.example.feature.aula.domain.usecases

import com.example.core.database.entity.Aluno
import com.example.feature.aula.domain.repository.AulaRepository

class UpdateAlunoUseCase(private val repository: AulaRepository) {
    suspend operator fun invoke(aluno: Aluno) = repository.updateAluno(aluno)
}
