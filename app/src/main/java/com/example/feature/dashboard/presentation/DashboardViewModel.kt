package com.example.feature.dashboard.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.preferences.AppPreferences
import com.example.feature.dashboard.data.repository.DashboardRepositoryImpl
import com.example.feature.dashboard.domain.repository.DashboardRepository
import com.example.feature.dashboard.domain.usecases.AddExtraTimeUseCase
import com.example.feature.dashboard.domain.usecases.GetActiveAulasUseCase
import com.example.feature.dashboard.domain.usecases.GetDashboardStatsUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    application: Application,
    private val repository: DashboardRepository,
    private val getActiveAulasUseCase: GetActiveAulasUseCase,
    private val getDashboardStatsUseCase: GetDashboardStatsUseCase,
    private val addExtraTimeUseCase: AddExtraTimeUseCase
) : AndroidViewModel(application) {

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getDatabase(application)
            val prefs = AppPreferences(application)
            val repo = DashboardRepositoryImpl(db, prefs)
            return DashboardViewModel(
                application = application,
                repository = repo,
                getActiveAulasUseCase = GetActiveAulasUseCase(repo),
                getDashboardStatsUseCase = GetDashboardStatsUseCase(repo),
                addExtraTimeUseCase = AddExtraTimeUseCase(repo, application)
            ) as T
        }
    }

    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())

    private val firstThreeCombined = combine(
        getActiveAulasUseCase(),
        repository.getAlunosFlow(),
        repository.getMotosFlow()
    ) { activeLessons, alunos, motos ->
        Triple(activeLessons, alunos, motos)
    }

    private val nextThreeCombined = combine(
        repository.getInstrutorFlow(),
        getDashboardStatsUseCase.getCompletedTodayCount(),
        getDashboardStatsUseCase.getPendingBackupCount()
    ) { instrutor, completedCount, pendingBackup ->
        Triple(instrutor, completedCount, pendingBackup)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        firstThreeCombined,
        nextThreeCombined
    ) { firstThree, nextThree ->
        DashboardUiState(
            activeLessons = firstThree.first,
            allAlunos = firstThree.second,
            allMotos = firstThree.third,
            currentInstrutor = nextThree.first,
            completedTodayCount = nextThree.second,
            pendingBackupCount = nextThree.third,
            lastBackupTime = repository.getGoogleLastSyncTime(),
            isGoogleBackupEnabled = repository.isGoogleBackupEnabled(),
            googleAccountEmail = repository.getGoogleAccountEmail()
        )
    }.combine(_currentTimeMillis) { state, currentTime ->
        state.copy(currentTimeMillis = currentTime)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DashboardUiState())

    init {
        // Start ticking timer
        viewModelScope.launch {
            while (true) {
                _currentTimeMillis.value = System.currentTimeMillis()
                delay(1000)
            }
        }
    }

    fun onEvent(event: DashboardUiEvent) {
        when (event) {
            is DashboardUiEvent.AddExtraTimeClick -> {
                viewModelScope.launch {
                    addExtraTimeUseCase(event.aulaId, event.minutes)
                }
            }
            DashboardUiEvent.RefreshTimerOnResume -> {
                _currentTimeMillis.value = System.currentTimeMillis()
            }
            else -> {
                // Other events like AddStudentClick or CheckoutStudentClick can be intercepted
                // by the Compose screen to perform navigation or delegate to AulaViewModel
            }
        }
    }
}
