package com.example.feature.cadastros.domain.usecases

import com.example.core.database.entity.Moto
import com.example.feature.cadastros.domain.repository.CadastrosRepository

class UpdateMotoUseCase(private val repository: CadastrosRepository) {
    suspend operator fun invoke(moto: Moto) {
        repository.updateMoto(moto)
    }
}
