package com.example.feature.cadastros.presentation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.database.entity.Agendamento
import com.example.core.preferences.AppPreferences
import com.example.feature.agenda.domain.repository.AgendaRepository
import com.example.feature.aula.presentation.receiver.AulaAlarmReceiver
import com.example.feature.cadastros.data.repository.CadastrosRepositoryImpl
import com.example.feature.cadastros.domain.usecases.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CadastrosViewModel(
    private val getAlunosUseCase: GetAlunosUseCase,
    private val getMotosUseCase: GetMotosUseCase,
    private val getAulasWithDetailsUseCase: GetAulasWithDetailsUseCase,
    private val addStudentUseCase: AddStudentUseCase,
    private val updateStudentUseCase: UpdateStudentUseCase,
    private val deleteStudentUseCase: DeleteStudentUseCase,
    private val addMotoUseCase: AddMotoUseCase,
    private val updateMotoUseCase: UpdateMotoUseCase,
    private val deleteMotoUseCase: DeleteMotoUseCase,
    private val agendaRepository: AgendaRepository,
    private val appContext: Context? = null
) : ViewModel() {

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getDatabase(context)
            val repo = CadastrosRepositoryImpl(db)
            val agendaRepo = com.example.feature.agenda.data.repository.AgendaRepositoryImpl(db)
            return CadastrosViewModel(
                getAlunosUseCase = GetAlunosUseCase(repo),
                getMotosUseCase = GetMotosUseCase(repo),
                getAulasWithDetailsUseCase = GetAulasWithDetailsUseCase(repo),
                addStudentUseCase = AddStudentUseCase(repo),
                updateStudentUseCase = UpdateStudentUseCase(repo),
                deleteStudentUseCase = DeleteStudentUseCase(repo),
                addMotoUseCase = AddMotoUseCase(repo),
                updateMotoUseCase = UpdateMotoUseCase(repo),
                deleteMotoUseCase = DeleteMotoUseCase(repo),
                agendaRepository = agendaRepo,
                appContext = context.applicationContext
            ) as T
        }
    }

    val uiState: StateFlow<CadastrosUiState> = combine(
        getAlunosUseCase(),
        getMotosUseCase(),
        getAulasWithDetailsUseCase()
    ) { alunos, motos, aulas ->
        CadastrosUiState(
            alunos = alunos,
            motos = motos,
            aulas = aulas,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CadastrosUiState(isLoading = true)
    )

    fun onEvent(event: CadastrosUiEvent) {
        viewModelScope.launch {
            when (event) {
                is CadastrosUiEvent.AddStudent -> {
                    val newId = addStudentUseCase(
                        nome = event.nome,
                        cpf = event.cpf,
                        telefone = event.telefone,
                        contratadas = event.contratadas,
                        realizadas = event.realizadas,
                        status = event.status,
                        exame = event.exame,
                        horaExame = event.horaExame,
                        obs = event.obs,
                        foto = event.foto
                    )
                    syncExameAgendamento(newId, event.nome, event.exame, event.horaExame)
                }
                is CadastrosUiEvent.UpdateStudent -> {
                    updateStudentUseCase(event.aluno)
                    syncExameAgendamento(
                        event.aluno.id,
                        event.aluno.nome,
                        event.aluno.dataExame,
                        event.aluno.horaExame
                    )
                }
                is CadastrosUiEvent.DeleteStudent -> {
                    cancelExamAlarmForAluno(event.aluno.id)
                    val errorMsg = deleteStudentUseCase(event.aluno, appContext)
                    if (errorMsg != null) {
                        _toastMessage.value = errorMsg
                    } else {
                        _toastMessage.value = "Aluno excluído com sucesso."
                    }
                }
                is CadastrosUiEvent.AddMoto -> {
                    addMotoUseCase(
                        marca = event.marca,
                        modelo = event.modelo,
                        ano = event.ano,
                        placa = event.placa,
                        km = event.km,
                        status = event.status,
                        foto = event.foto
                    )
                }
                is CadastrosUiEvent.UpdateMoto -> {
                    updateMotoUseCase(event.moto)
                }
                is CadastrosUiEvent.DeleteMoto -> {
                    deleteMotoUseCase(event.moto)
                }
            }
        }
    }

    fun createPhotoFile(context: Context, prefix: String): File {
        val prefs = AppPreferences(context)
        val dateDirStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val folderName = when {
            prefix.contains("instrutor") -> {
                "Instrutor_Sessao"
            }
            prefix.contains("aluno") || prefix.contains("add_aluno") -> {
                val activeAlId = if (prefix.contains("add_aluno")) {
                    prefs.addStudentSelectedId
                } else {
                    prefs.activeAlunoId
                }
                if (activeAlId != -1L) {
                    "Aluno_$activeAlId"
                } else {
                    "Aluno_Geral"
                }
            }
            else -> {
                val activeAlId = prefs.activeAlunoId
                if (activeAlId != -1L) {
                    "Aluno_$activeAlId"
                } else {
                    "Geral"
                }
            }
        }

        val storageDir = File(File(context.filesDir, "photos"), "$folderName/$dateDirStr")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val uniqueId = UUID.randomUUID().toString().take(6)
        return File(storageDir, "${prefix}_${timestamp}_${uniqueId}.jpg")
    }

    /**
     * Sincroniza o agendamento EXAME na Agenda conforme data/hora do exame do aluno.
     * - Se data e hora validas: cria ou atualiza Agendamento tipo=EXAME + agenda alarme 24h antes
     * - Se falta data ou hora: remove Agendamento EXAME existente + cancela alarme
     *
     * Usa a MESMA infraestrutura de alarme da AgendaViewModel (requestCode = agendamentoId + 5_000_000L,
     * AulaAlarmReceiver, 24h antes do horario real).
     */
    private suspend fun syncExameAgendamento(alunoId: Long, alunoNome: String, dataExame: String, horaExame: String) {
        // Cancelar alarme e remover agendamento EXAME existente
        val existing = agendaRepository.getExameAgendamentoByAlunoId(alunoId)
        if (existing != null) {
            cancelExamAlarm(existing.id)
            agendaRepository.deleteAgendamentoById(existing.id)
        }

        // Validar data e hora - ambos precisam estar preenchidos e em formato valido
        val dataHora = parseExamDateTime(dataExame, horaExame) ?: return

        // Criar novo Agendamento EXAME (sem moto - motoId null)
        val novoExame = Agendamento(
            alunoId = alunoId,
            motoId = null,
            dataHora = dataHora,
            status = "agendada",
            observacoes = "EXAME",
            tipo = "EXAME"
        )
        val novoId = agendaRepository.insertAgendamento(novoExame)

        // Agendar alarme 24h antes, usando MESMA faixa de requestCode da Agenda (agendamentoId + 5_000_000L)
        scheduleExamAlarm(novoId, alunoNome, dataHora)

        agendaRepository.auditLog(
            "exame_agendado",
            "Exame agendado para aluno ID $alunoId ($alunoNome) em ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(dataHora))}."
        )
    }

    /**
     * Converte data (dd/MM/yyyy) + hora (HH:mm) em timestamp UTC do timezone local.
     * Retorna null se formato invalido ou campos vazios.
     */
    private fun parseExamDateTime(dataExame: String, horaExame: String): Long? {
        val dataClean = dataExame.trim()
        val horaClean = horaExame.trim()
        if (dataClean.isEmpty() || horaClean.isEmpty()) return null

        val parts = dataClean.split("/")
        if (parts.size != 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val year = parts[2].toIntOrNull() ?: return null
        if (day !in 1..31 || month !in 1..12 || year < 2000) return null

        val hp = horaClean.split(":")
        if (hp.size != 2) return null
        val hh = hp[0].toIntOrNull() ?: return null
        val mm = hp[1].toIntOrNull() ?: return null
        if (hh !in 0..23 || mm !in 0..59) return null

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hh)
            set(Calendar.MINUTE, mm)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Agenda alarme 24h antes do horario real do exame.
     * Usa a MESMA faixa de requestCode do AgendaViewModel (agendamentoId + 5_000_000L).
     */
    private fun scheduleExamAlarm(agendamentoId: Long, alunoNome: String, dataHora: Long) {
        if (appContext == null) return
        val triggerTime = dataHora - (24L * 60 * 60 * 1000L) // 24h antes
        if (triggerTime <= System.currentTimeMillis()) return

        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val horaStr = sdf.format(Date(dataHora))

        val intent = Intent(appContext, AulaAlarmReceiver::class.java).apply {
            putExtra("title", "Aviso de Exame")
            putExtra("message", "Amanhã é o exame do(a) aluno(a) $alunoNome às $horaStr.")
            putExtra("aulaId", agendamentoId + 5_000_000L)
            putExtra("alertType", "exame_reminder")
            putExtra("soundDurationMs", 500)
            putExtra("vibeDurationMs", 300)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            (agendamentoId + 5_000_000L).toInt(),
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
            Log.e("CadastrosViewModel", "Failed to schedule exam alarm", e)
        }
    }

    /**
     * Cancela alarme de um agendamento EXAME especifico pelo seu ID.
     */
    private fun cancelExamAlarm(agendamentoId: Long) {
        if (appContext == null) return
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(appContext, AulaAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            (agendamentoId + 5_000_000L).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Cancela qualquer alarme de EXAME associado ao aluno.
     * Busca o Agendamento EXAME atual (se existir) e cancela pelo seu ID.
     * Usado quando o aluno esta sendo excluido.
     */
    private suspend fun cancelExamAlarmForAluno(alunoId: Long) {
        val existing = agendaRepository.getExameAgendamentoByAlunoId(alunoId)
        if (existing != null) {
            cancelExamAlarm(existing.id)
            agendaRepository.deleteAgendamentoById(existing.id)
        }
    }
}
