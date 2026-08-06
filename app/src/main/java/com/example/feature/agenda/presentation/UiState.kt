package com.example.feature.agenda.presentation

import com.example.core.database.entity.Aluno
import com.example.core.database.entity.Moto
import com.example.core.database.dao.AgendamentoWithDetails

data class AgendaUiState(
    val agendamentos: List<AgendamentoWithDetails> = emptyList(),
    val alunos: List<Aluno> = emptyList(),
    val motos: List<Moto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
