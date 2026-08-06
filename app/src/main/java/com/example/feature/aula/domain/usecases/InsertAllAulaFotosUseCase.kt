package com.example.feature.aula.domain.usecases

import com.example.core.database.entity.AulaFoto
import com.example.feature.aula.domain.repository.AulaRepository

class InsertAllAulaFotosUseCase(private val repository: AulaRepository) {
    suspend operator fun invoke(aulaFotos: List<AulaFoto>) = repository.insertAllAulaFotos(aulaFotos)
}
