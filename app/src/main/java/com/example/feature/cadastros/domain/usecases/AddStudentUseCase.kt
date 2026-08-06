package com.example.feature.cadastros.domain.usecases

import com.example.core.database.entity.Aluno
import com.example.feature.cadastros.domain.repository.CadastrosRepository
import java.text.SimpleDateFormat
import java.util.*

class AddStudentUseCase(private val repository: CadastrosRepository) {
    suspend operator fun invoke(
        nome: String,
        cpf: String,
        telefone: String,
        contratadas: Int,
        realizadas: Int,
        status: String,
        exame: String,
        obs: String,
        foto: String
    ): Long {
        val al = Aluno(
            nome = nome,
            cpf = cpf,
            telefone = telefone,
            aulasContratadas = contratadas,
            aulasRealizadas = realizadas,
            status = status,
            dataExame = examenFallback(exame),
            observacoes = obs,
            fotoCadastro = foto
        )
        return repository.insertAluno(al)
    }

    private fun examenFallback(date: String): String {
        return date.ifEmpty {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)) // 30 days from now
        }
    }
}
