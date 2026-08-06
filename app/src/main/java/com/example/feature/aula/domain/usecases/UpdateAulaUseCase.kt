package com.example.feature.aula.domain.usecases

import com.example.core.database.entity.Aula
import com.example.feature.aula.domain.repository.AulaRepository

class UpdateAulaUseCase(private val repository: AulaRepository) {
    suspend operator fun invoke(aula: Aula) = repository.updateAula(aula)
}
