package com.example.feature.cadastros.domain.usecases

import com.example.core.database.entity.Moto
import com.example.feature.cadastros.domain.repository.CadastrosRepository

class DeleteMotoUseCase(private val repository: CadastrosRepository) {
    suspend operator fun invoke(moto: Moto) {
        repository.deleteMoto(moto)
    }
}
