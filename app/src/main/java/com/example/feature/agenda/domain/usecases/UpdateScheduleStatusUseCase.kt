package com.example.feature.agenda.domain.usecases

import com.example.core.database.entity.Agendamento
import com.example.feature.agenda.domain.repository.AgendaRepository
import kotlinx.coroutines.flow.first

class UpdateScheduleStatusUseCase(private val repository: AgendaRepository) {
    suspend operator fun invoke(agendamentoId: Long, status: String) {
        val agendamentos = repository.getAgendamentosWithDetailsFlow().first()
        val existing = agendamentos.find { it.id == agendamentoId }
        if (existing != null) {
            val updated = Agendamento(
                id = existing.id,
                alunoId = existing.alunoId,
                motoId = existing.motoId,
                dataHora = existing.dataHora,
                status = status,
                observacoes = existing.observacoes
            )
            repository.updateAgendamento(updated)
            repository.auditLog("agendamento_atualizado", "Status do agendamento $agendamentoId alterado para $status.")
        }
    }
}
