package com.example.feature.cadastros.presentation
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.notifications.ScheduleAlarmUseCase
import com.example.core.preferences.AppPreferences
import com.example.feature.agenda.data.repository.AgendaRepositoryImpl
import com.example.feature.agenda.domain.repository.AgendaRepository
import com.example.feature.agenda.domain.usecases.ScheduleClassUseCase
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
    private val scheduleClassUseCase: ScheduleClassUseCase,
    private val agendaRepository: AgendaRepository,
    private val scheduleAlarmUseCase: ScheduleAlarmUseCase,
    private val appContext: Context? = null
) : ViewModel() {
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()
    fun clearToastMessage() { _toastMessage.value = null }
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val ac = context.applicationContext
            val db = AppDatabase.getDatabase(ac)
            val r = CadastrosRepositoryImpl(db)
            val ar = AgendaRepositoryImpl(db)
            return CadastrosViewModel(GetAlunosUseCase(r), GetMotosUseCase(r), GetAulasWithDetailsUseCase(r), AddStudentUseCase(r), UpdateStudentUseCase(r), DeleteStudentUseCase(r, ar), AddMotoUseCase(r), UpdateMotoUseCase(r), DeleteMotoUseCase(r), ScheduleClassUseCase(ar), ar, ScheduleAlarmUseCase(ac), ac) as T
        }
    }
    val uiState: StateFlow<CadastrosUiState> = combine(getAlunosUseCase(), getMotosUseCase(), getAulasWithDetailsUseCase()) { a, m, au -> CadastrosUiState(a, m, au, false) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CadastrosUiState(isLoading = true))
    fun onEvent(event: CadastrosUiEvent) {
        viewModelScope.launch {
            when (event) {
                is CadastrosUiEvent.AddStudent -> {
                    val nid = addStudentUseCase(event.nome, event.cpf, event.telefone, event.contratadas, event.realizadas, event.status, event.exame, event.horaExame, event.obs, event.foto)
                    syncExame(nid, event.nome, event.exame, event.horaExame)
                }
                is CadastrosUiEvent.UpdateStudent -> {
                    updateStudentUseCase(event.aluno)
                    syncExame(event.aluno.id, event.aluno.nome, event.aluno.dataExame, event.aluno.horaExame)
                }
                is CadastrosUiEvent.DeleteStudent -> { val m = deleteStudentUseCase(event.aluno, appContext); _toastMessage.value = m ?: "Aluno excluido." }
                is CadastrosUiEvent.AddMoto -> { addMotoUseCase(event.marca, event.modelo, event.ano, event.placa, event.km, event.status, event.foto) }
                is CadastrosUiEvent.UpdateMoto -> { updateMotoUseCase(event.moto) }
                is CadastrosUiEvent.DeleteMoto -> { deleteMotoUseCase(event.moto) }
            }
        }
    }
    private suspend fun syncExame(aid: Long, anome: String, dexame: String, hexame: String) {
        if (dexame.isBlank()) { val ex = agendaRepository.getExameAgendamentoByAlunoId(aid); if (ex != null) { scheduleAlarmUseCase.cancel(ex.id); agendaRepository.deleteAgendamentoById(ex.id) }; return }
        val dh = parseEx(dexame, hexame) ?: return
        val ex = agendaRepository.getExameAgendamentoByAlunoId(aid)
        if (ex != null) { agendaRepository.updateAgendamento(ex.copy(dataHora = dh, status = "agendada")); scheduleAlarmUseCase.schedule(ex.id, anome, dh, "EXAME") }
        else { val nid = scheduleClassUseCase(aid, null, dh, "EXAME $anome", 0L, "EXAME"); scheduleAlarmUseCase.schedule(nid, anome, dh, "EXAME") }
    }
    private fun parseEx(d: String, h: String): Long? {
        val p = d.trim().split("/"); if (p.size != 3) return null
        val dd = p[0].toIntOrNull() ?: return null; val mm = p[1].toIntOrNull() ?: return null; val yy = p[2].toIntOrNull() ?: return null
        if (dd !in 1..31 || mm !in 1..12 || yy < 2000) return null
        var hh = 8; var mi = 0
        val ch = h.trim()
        if (ch.isNotEmpty()) { val hp = ch.split(":"); if (hp.size == 2) { val a = hp[0].toIntOrNull(); val b = hp[1].toIntOrNull(); if (a != null && b != null && a in 0..23 && b in 0..59) { hh = a; mi = b } } }
        val c = Calendar.getInstance(); c.set(Calendar.YEAR, yy); c.set(Calendar.MONTH, mm - 1); c.set(Calendar.DAY_OF_MONTH, dd); c.set(Calendar.HOUR_OF_DAY, hh); c.set(Calendar.MINUTE, mi); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
    fun createPhotoFile(context: Context, prefix: String): File {
        val prefs = AppPreferences(context)
        val ds = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val fn = when {
            prefix.contains("instrutor") -> "Instrutor_Sessao"
            prefix.contains("aluno") || prefix.contains("add_aluno") -> { val al = if (prefix.contains("add_aluno")) prefs.addStudentSelectedId else prefs.activeAlunoId; if (al != -1L) "Aluno_$al" else "Aluno_Geral" }
            else -> { val al = prefs.activeAlunoId; if (al != -1L) "Aluno_$al" else "Geral" }
        }
        val sd = File(File(context.filesDir, "photos"), "$fn/$ds")
        if (!sd.exists()) sd.mkdirs()
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(sd, "${prefix}_${ts}_${UUID.randomUUID().toString().take(6)}.jpg")
    }
}
