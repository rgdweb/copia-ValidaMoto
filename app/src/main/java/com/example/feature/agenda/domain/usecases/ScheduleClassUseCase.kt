package com.example.feature.agenda.domain.usecases

import com.example.core.database.entity.Agendamento
import com.example.feature.agenda.domain.repository.AgendaRepository

class ScheduleClassUseCase(private val repository: AgendaRepository) {
    suspend operator fun invoke(alunoId: Long, motoId: Long, dateTimestamp: Long, observacoes: String, id: Long = 0L): Long {
        val agendamento = Agendamento(
            id = id,
            alunoId = alunoId,
            motoId = motoId,
            dataHora = dateTimestamp,
            status = "agendada",
            observacoes = observacoes
        )
        val resultId = repository.insertAgendamento(agendamento)
        val action = if (id == 0L) "agendamento_criado" else "agendamento_editado"
        val desc = if (id == 0L) "Nova aula agendada para o aluno ID $alunoId." else "Agendamento ID $resultId editado."
        repository.auditLog(action, desc)
        return resultId
    }
}
