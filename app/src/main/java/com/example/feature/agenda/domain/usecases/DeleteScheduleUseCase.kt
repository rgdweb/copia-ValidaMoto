package com.example.feature.agenda.domain.usecases
import com.example.core.database.entity.Agendamento
import com.example.feature.agenda.domain.repository.AgendaRepository
import kotlinx.coroutines.flow.first
class DeleteScheduleUseCase(private val repository: AgendaRepository) {
    suspend operator fun invoke(agendamentoId: Long) {
        val ag = repository.getAgendamentosWithDetailsFlow().first().find { it.id == agendamentoId }
        if (ag != null) {
            repository.deleteAgendamento(Agendamento(id = ag.id, alunoId = ag.alunoId, motoId = ag.motoId, dataHora = ag.dataHora, status = ag.status, observacoes = ag.observacoes, tipo = ag.tipo))
            repository.auditLog("agendamento_excluido", "Ag $agendamentoId excluido.")
        }
    }
}
