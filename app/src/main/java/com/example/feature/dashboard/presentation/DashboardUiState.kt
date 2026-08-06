package com.example.feature.dashboard.presentation

import com.example.core.database.entity.*
import com.example.core.database.dao.AulaWithDetails

data class DashboardUiState(
    val activeLessons: List<AulaWithDetails> = emptyList(),
    val allAlunos: List<Aluno> = emptyList(),
    val allMotos: List<Moto> = emptyList(),
    val currentInstrutor: Instrutor? = null,
    val completedTodayCount: Int = 0,
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val pendingBackupCount: Int = 0,
    val lastBackupTime: String = "Nunca",
    val isGoogleBackupEnabled: Boolean = false,
    val googleAccountEmail: String? = null
)
