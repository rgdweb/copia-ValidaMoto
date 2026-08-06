package com.example.feature.cadastros.domain.usecases

import com.example.core.database.entity.Aluno
import com.example.feature.cadastros.domain.repository.CadastrosRepository

class UpdateStudentUseCase(private val repository: CadastrosRepository) {
    suspend operator fun invoke(aluno: Aluno) {
        repository.updateAluno(aluno)
    }
}
