package com.example.feature.cadastros.domain.usecases
import android.content.Context
import com.example.core.database.entity.Aluno
import com.example.core.notifications.ScheduleAlarmUseCase
import com.example.core.preferences.AppPreferences
import com.example.feature.cadastros.domain.repository.CadastrosRepository
import com.example.feature.agenda.domain.repository.AgendaRepository
import java.io.File
class DeleteStudentUseCase(private val repository: CadastrosRepository, private val agendaRepository: AgendaRepository) {
    suspend operator fun invoke(aluno: Aluno, context: Context? = null): String? {
        val aa = repository.countActiveAulasByAluno(aluno.id)
        if (aa > 0) return "Nao e possivel excluir: aula em andamento."
        if (context != null) { val p = AppPreferences(context); if (p.activeAlunoId == aluno.id && p.activeStep != 0) return "Nao e possivel excluir: aula em andamento." }
        val tag = repository.countAgendamentosByAluno(aluno.id)
        val ea = repository.getExameAgendamentoByAlunoId(aluno.id)
        val aag = if (ea != null) tag - 1 else tag
        if (aag > 0) return "Nao e possivel excluir: agendamentos na agenda."
        val ta = repository.countAulasByAluno(aluno.id)
        if (ta > 0) return "Nao e possivel excluir: historico de aulas."
        return try {
            if (ea != null && context != null) { val s = ScheduleAlarmUseCase(context); s.cancel(ea.id); agendaRepository.deleteAgendamentoById(ea.id) }
            repository.deleteAluno(aluno)
            if (aluno.fotoCadastro.isNotEmpty()) { try { val f = File(aluno.fotoCadastro); if (f.exists()) f.delete() } catch (_: Exception) {} }
            if (context != null) { try { val sf = File(File(context.filesDir, "photos"), "Aluno_${aluno.id}"); if (sf.exists()) sf.deleteRecursively() } catch (_: Exception) {} }
            null
        } catch (e: Exception) { "Erro: ${e.localizedMessage}" }
    }
}
