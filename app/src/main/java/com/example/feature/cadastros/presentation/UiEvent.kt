package com.example.feature.cadastros.presentation

import com.example.core.database.entity.Aluno
import com.example.core.database.entity.Moto

sealed interface CadastrosUiEvent {
    data class AddStudent(
        val nome: String,
        val cpf: String,
        val telefone: String,
        val contratadas: Int,
        val realizadas: Int,
        val status: String,
        val exame: String,
        val obs: String,
        val foto: String
    ) : CadastrosUiEvent

    data class UpdateStudent(
        val aluno: Aluno
    ) : CadastrosUiEvent

    data class DeleteStudent(
        val aluno: Aluno
    ) : CadastrosUiEvent

    data class AddMoto(
        val marca: String,
        val modelo: String,
        val ano: Int,
        val placa: String,
        val km: Int,
        val status: String,
        val foto: String
    ) : CadastrosUiEvent

    data class UpdateMoto(
        val moto: Moto
    ) : CadastrosUiEvent

    data class DeleteMoto(
        val moto: Moto
    ) : CadastrosUiEvent
}
