package com.example.feature.cadastros.domain.usecases

import android.content.Context
import com.example.core.database.entity.Aluno
import com.example.core.preferences.AppPreferences
import com.example.feature.cadastros.domain.repository.CadastrosRepository
import java.io.File

class DeleteStudentUseCase(private val repository: CadastrosRepository) {
    suspend operator fun invoke(aluno: Aluno, context: Context? = null): String? {
        // Cenário 4 — Aluno participante de sessão/aula ativa
        val activeAulas = repository.countActiveAulasByAluno(aluno.id)
        if (activeAulas > 0) {
            return "Não é possível excluir o aluno pois ele possui uma aula em andamento."
        }

        if (context != null) {
            val prefs = AppPreferences(context)
            if (prefs.activeAlunoId == aluno.id && prefs.activeStep != 0) {
                return "Não é possível excluir o aluno pois ele possui uma aula em andamento."
            }
        }

        // Cenário 3 — Aluno com agendamentos vinculados
        val agendamentos = repository.countAgendamentosByAluno(aluno.id)
        if (agendamentos > 0) {
            return "Não é possível excluir o aluno pois ele possui agendamentos na agenda."
        }

        // Cenário 2 — Aluno com aulas vinculadas
        val totalAulas = repository.countAulasByAluno(aluno.id)
        if (totalAulas > 0) {
            return "Não é possível excluir o aluno pois ele possui histórico de aulas vinculadas."
        }

        // Cenário 1 e 5 — Exclusão permitida
        return try {
            repository.deleteAluno(aluno)

            // Limpeza de recursos locais associados exclusivamente ao aluno
            if (aluno.fotoCadastro.isNotEmpty()) {
                try {
                    val photoFile = File(aluno.fotoCadastro)
                    if (photoFile.exists()) {
                        photoFile.delete()
                    }
                } catch (_: Exception) {}
            }

            if (context != null) {
                try {
                    val studentFolder = File(File(context.filesDir, "photos"), "Aluno_${aluno.id}")
                    if (studentFolder.exists()) {
                        studentFolder.deleteRecursively()
                    }
                } catch (_: Exception) {}
            }

            null // Sucesso
        } catch (e: Exception) {
            "Não foi possível excluir o aluno: ${e.localizedMessage ?: "Erro de vínculo."}"
        }
    }
}
