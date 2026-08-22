package com.example.feature.agenda.presentation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.agenda.domain.usecases.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import android.content.Context
import com.example.core.database.AppDatabase
import com.example.core.notifications.ScheduleAlarmUseCase
import com.example.feature.agenda.data.repository.AgendaRepositoryImpl
class AgendaViewModel(
    private val getAgendamentosWithDetailsUseCase: GetAgendamentosWithDetailsUseCase,
    private val getAlunosUseCase: GetAlunosUseCase,
    private val getMotosUseCase: GetMotosUseCase,
    private val scheduleClassUseCase: ScheduleClassUseCase,
    private val updateScheduleStatusUseCase: UpdateScheduleStatusUseCase,
    private val deleteScheduleUseCase: DeleteScheduleUseCase,
    private val scheduleAlarmUseCase: ScheduleAlarmUseCase
) : ViewModel() {
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val ac = context.applicationContext
            val db = AppDatabase.getDatabase(ac)
            val r = AgendaRepositoryImpl(db)
            return AgendaViewModel(GetAgendamentosWithDetailsUseCase(r), GetAlunosUseCase(r), GetMotosUseCase(r), ScheduleClassUseCase(r), UpdateScheduleStatusUseCase(r), DeleteScheduleUseCase(r), ScheduleAlarmUseCase(ac)) as T
        }
    }
    val uiState: StateFlow<AgendaUiState> = combine(getAgendamentosWithDetailsUseCase(), getAlunosUseCase(), getMotosUseCase()) { a, m, au -> AgendaUiState(a, m, au, false) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AgendaUiState(isLoading = true))
    fun onEvent(event: AgendaUiEvent) {
        viewModelScope.launch {
            when (event) {
                is AgendaUiEvent.ScheduleClass -> {
                    val aid = scheduleClassUseCase(event.alunoId, event.motoId, event.timestamp, event.observacoes, event.id, event.tipo)
                    val al = uiState.value.alunos.find { it.id == event.alunoId }
                    scheduleAlarmUseCase.schedule(if (event.id != 0L) event.id else aid, al?.nome ?: "Aluno", event.timestamp, event.tipo)
                }
                is AgendaUiEvent.UpdateStatus -> {
                    updateScheduleStatusUseCase(event.agendamentoId, event.status)
                    if (event.status != "agendada") scheduleAlarmUseCase.cancel(event.agendamentoId)
                    else { val a = uiState.value.agendamentos.find { it.id == event.agendamentoId }; if (a != null) scheduleAlarmUseCase.schedule(a.id, a.alunoNome, a.dataHora, a.tipo) }
                }
                is AgendaUiEvent.DeleteSchedule -> { scheduleAlarmUseCase.cancel(event.agendamentoId); deleteScheduleUseCase(event.agendamentoId) }
            }
        }
    }
}
