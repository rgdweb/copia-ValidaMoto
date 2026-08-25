package com.example.feature.cadastros.domain.usecases

import com.example.core.database.entity.Aluno
import com.example.feature.cadastros.domain.repository.CadastrosRepository

class AddStudentUseCase(private val repository: CadastrosRepository) {
    suspend operator fun invoke(
        nome: String,
        cpf: String,
        telefone: String,
        contratadas: Int,
        realizadas: Int,
        status: String,
        exame: String,
        horaExame: String,
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
            dataExame = exame.trim(),
            horaExame = horaExame.trim(),
            observacoes = obs,
            fotoCadastro = foto
        )
        return repository.insertAluno(al)
    }
}
