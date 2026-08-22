package com.example.feature.agenda.presentation
sealed interface AgendaUiEvent {
    data class ScheduleClass(val alunoId: Long, val motoId: Long?, val timestamp: Long, val observacoes: String, val id: Long = 0L, val tipo: String = "AULA") : AgendaUiEvent
    data class UpdateStatus(val agendamentoId: Long, val status: String) : AgendaUiEvent
    data class DeleteSchedule(val agendamentoId: Long) : AgendaUiEvent
}
