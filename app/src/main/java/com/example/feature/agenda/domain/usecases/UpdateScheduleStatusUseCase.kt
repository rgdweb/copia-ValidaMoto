package com.example.feature.agenda.domain.usecases
import com.example.core.database.entity.Agendamento
import com.example.feature.agenda.domain.repository.AgendaRepository
import kotlinx.coroutines.flow.first
class UpdateScheduleStatusUseCase(private val repository: AgendaRepository) {
    suspend operator fun invoke(agendamentoId: Long, status: String) {
        val ag = repository.getAgendamentosWithDetailsFlow().first().find { it.id == agendamentoId }
        if (ag != null) {
            repository.updateAgendamento(Agendamento(id = ag.id, alunoId = ag.alunoId, motoId = ag.motoId, dataHora = ag.dataHora, status = status, observacoes = ag.observacoes, tipo = ag.tipo))
            repository.auditLog("agendamento_atualizado", "Status $agendamentoId -> $status.")
        }
    }
}
