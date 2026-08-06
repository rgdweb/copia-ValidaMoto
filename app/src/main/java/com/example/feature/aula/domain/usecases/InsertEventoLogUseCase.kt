package com.example.feature.aula.domain.usecases

import com.example.core.database.entity.EventoLog
import com.example.feature.aula.domain.repository.AulaRepository

class InsertEventoLogUseCase(private val repository: AulaRepository) {
    suspend operator fun invoke(log: EventoLog): Long = repository.insertEventoLog(log)
}
