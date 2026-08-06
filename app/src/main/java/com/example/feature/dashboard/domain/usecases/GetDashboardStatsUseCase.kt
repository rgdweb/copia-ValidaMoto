package com.example.feature.dashboard.domain.usecases

import com.example.core.database.dao.AulaWithDetails
import com.example.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

class GetDashboardStatsUseCase(private val repository: DashboardRepository) {
    fun getCompletedTodayCount(): Flow<Int> {
        return repository.getAulasWithDetailsFlow().map { list ->
            val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            list.count {
                it.dataHoraFim > 0L && 
                it.statusAula == "confirmada" &&
                SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.dataHoraInicio)) == todayStr
            }
        }
    }

    fun getPendingBackupCount(): Flow<Int> {
        return repository.getAulasWithDetailsFlow().map { list ->
            list.count { it.statusAula == "pendente" && it.dataHoraFim > 0L }
        }
    }
}
