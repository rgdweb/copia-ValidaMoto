package com.example.feature.cadastros.presentation

import com.example.core.database.entity.Aluno
import com.example.core.database.entity.Moto
import com.example.core.database.dao.AulaWithDetails

data class CadastrosUiState(
    val alunos: List<Aluno> = emptyList(),
    val motos: List<Moto> = emptyList(),
    val aulas: List<AulaWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
