package com.example.feature.agenda.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.agenda.domain.usecases.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import androidx.lifecycle.ViewModelProvider
import android.content.Context
import com.example.core.database.AppDatabase
import com.example.feature.agenda.data.repository.AgendaRepositoryImpl

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.feature.aula.presentation.receiver.AulaAlarmReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgendaViewModel(
    private val context: Context,
    private val getAgendamentosWithDetailsUseCase: GetAgendamentosWithDetailsUseCase,
    private val getAlunosUseCase: GetAlunosUseCase,
    private val getMotosUseCase: GetMotosUseCase,
    private val scheduleClassUseCase: ScheduleClassUseCase,
    private val updateScheduleStatusUseCase: UpdateScheduleStatusUseCase,
    private val deleteScheduleUseCase: DeleteScheduleUseCase
) : ViewModel() {

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appContext = context.applicationContext
            val db = AppDatabase.getDatabase(appContext)
            val repo = AgendaRepositoryImpl(db)
            return AgendaViewModel(
                context = appContext,
                getAgendamentosWithDetailsUseCase = GetAgendamentosWithDetailsUseCase(repo),
                getAlunosUseCase = GetAlunosUseCase(repo),
                getMotosUseCase = GetMotosUseCase(repo),
                scheduleClassUseCase = ScheduleClassUseCase(repo),
                updateScheduleStatusUseCase = UpdateScheduleStatusUseCase(repo),
                deleteScheduleUseCase = DeleteScheduleUseCase(repo)
            ) as T
        }
    }

    val uiState: StateFlow<AgendaUiState> = combine(
        getAgendamentosWithDetailsUseCase(),
        getAlunosUseCase(),
        getMotosUseCase()
    ) { agendamentos, alunos, motos ->
        AgendaUiState(
            agendamentos = agendamentos,
            alunos = alunos,
            motos = motos,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AgendaUiState(isLoading = true)
    )

    fun onEvent(event: AgendaUiEvent) {
        viewModelScope.launch {
            when (event) {
                is AgendaUiEvent.ScheduleClass -> {
                    val agendamentoId = scheduleClassUseCase(
                        alunoId = event.alunoId,
                        motoId = event.motoId,
                        dateTimestamp = event.timestamp,
                        observacoes = event.observacoes,
                        id = event.id
                    )
                    val aluno = uiState.value.alunos.find { it.id == event.alunoId }
                    scheduleAgendaNotification(
                        agendamentoId = if (event.id != 0L) event.id else agendamentoId,
                        alunoNome = aluno?.nome ?: "Aluno",
                        dataHora = event.timestamp
                    )
                }
                is AgendaUiEvent.UpdateStatus -> {
                    updateScheduleStatusUseCase(
                        agendamentoId = event.agendamentoId,
                        status = event.status
                    )
                    if (event.status != "agendada") {
                        cancelAgendaNotification(event.agendamentoId)
                    } else {
                        val agendamento = uiState.value.agendamentos.find { it.id == event.agendamentoId }
                        if (agendamento != null) {
                            scheduleAgendaNotification(agendamento.id, agendamento.alunoNome, agendamento.dataHora)
                        }
                    }
                }
                is AgendaUiEvent.DeleteSchedule -> {
                    cancelAgendaNotification(event.agendamentoId)
                    deleteScheduleUseCase(agendamentoId = event.agendamentoId)
                }
            }
        }
    }

    private fun scheduleAgendaNotification(agendamentoId: Long, alunoNome: String, dataHora: Long) {
        cancelAgendaNotification(agendamentoId)
        val triggerTime = dataHora - (24 * 60 * 60 * 1000L)
        if (triggerTime <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val horaStr = sdf.format(Date(dataHora))

        val intent = Intent(context, AulaAlarmReceiver::class.java).apply {
            putExtra("title", "Aviso de Aula Agendada")
            putExtra("message", "Amanhã você possui uma aula agendada com $alunoNome às $horaStr.")
            putExtra("aulaId", agendamentoId + 5000000L)
            putExtra("alertType", "agenda_reminder")
            putExtra("soundDurationMs", 500)
            putExtra("vibeDurationMs", 300)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (agendamentoId + 5000000L).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("AgendaViewModel", "Failed to schedule notification alarm", e)
        }
    }

    private fun cancelAgendaNotification(agendamentoId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AulaAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (agendamentoId + 5000000L).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
