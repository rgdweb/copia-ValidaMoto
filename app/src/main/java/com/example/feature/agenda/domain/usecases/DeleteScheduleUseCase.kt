package com.example.feature.agenda.domain.usecases

import com.example.core.database.entity.Agendamento
import com.example.feature.agenda.domain.repository.AgendaRepository
import kotlinx.coroutines.flow.first

class DeleteScheduleUseCase(private val repository: AgendaRepository) {
    suspend operator fun invoke(agendamentoId: Long) {
        val agendamentos = repository.getAgendamentosWithDetailsFlow().first()
        val existing = agendamentos.find { it.id == agendamentoId }
        if (existing != null) {
            val item = Agendamento(
                id = existing.id,
                alunoId = existing.alunoId,
                motoId = existing.motoId,
                dataHora = existing.dataHora,
                status = existing.status,
                observacoes = existing.observacoes
            )
            repository.deleteAgendamento(item)
            repository.auditLog("agendamento_excluido", "Agendamento $agendamentoId excluído.")
        }
    }
}
