package com.example.feature.dashboard.domain.usecases

import android.app.Application
import android.content.Context
import com.example.feature.dashboard.domain.repository.DashboardRepository

class AddExtraTimeUseCase(
    private val repository: DashboardRepository,
    private val application: Application
) {
    suspend operator fun invoke(aulaId: Long, extraMinutes: Int) {
        val aula = repository.getAulaById(aulaId)
        if (aula != null) {
            val newDuration = (aula.duracaoMinutos + extraMinutes).coerceAtMost(120)
            repository.updateAula(aula.copy(duracaoMinutos = newDuration))

            // Clear alerts in shared preferences
            val sharedPrefs = application.getSharedPreferences("valida_moto_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .remove("alert_${aulaId}_10min")
                .remove("alert_${aulaId}_5min")
                .remove("alert_${aulaId}_1min")
                .remove("alert_${aulaId}_concluido")
                .commit()
        }
    }
}
