package com.example.feature.agenda.domain.usecases
import com.example.core.database.entity.Agendamento
import com.example.feature.agenda.domain.repository.AgendaRepository
class ScheduleClassUseCase(private val repository: AgendaRepository) {
    suspend operator fun invoke(alunoId: Long, motoId: Long?, dateTimestamp: Long, observacoes: String, id: Long = 0L, tipo: String = "AULA"): Long {
        val a = Agendamento(id = id, alunoId = alunoId, motoId = motoId, dataHora = dateTimestamp, status = "agendada", observacoes = observacoes, tipo = tipo)
        val rid = repository.insertAgendamento(a)
        val act = if (id == 0L) "agendamento_criado" else "agendamento_editado"
        val d = when (tipo) { "EXAME" -> if (id == 0L) "Exame $alunoId." else "Exame $rid editado." else -> if (id == 0L) "Aula $alunoId." else "Ag $rid editado." }
        repository.auditLog(act, d)
        return rid
    }
}
