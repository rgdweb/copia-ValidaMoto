package com.example.feature.aula.domain.usecases

import com.example.core.database.entity.Moto
import com.example.feature.aula.domain.repository.AulaRepository

class UpdateMotoUseCase(private val repository: AulaRepository) {
    suspend operator fun invoke(moto: Moto) = repository.updateMoto(moto)
}
