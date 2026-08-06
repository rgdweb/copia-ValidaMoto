package com.example.feature.aula.domain.usecases

import com.example.core.database.entity.AulaFoto
import com.example.feature.aula.domain.repository.AulaRepository

class InsertAulaFotoUseCase(private val repository: AulaRepository) {
    suspend operator fun invoke(aulaFoto: AulaFoto): Long = repository.insertAulaFoto(aulaFoto)
}
