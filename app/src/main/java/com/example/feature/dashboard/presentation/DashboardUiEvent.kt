package com.example.feature.dashboard.presentation

sealed interface DashboardUiEvent {
    object AddStudentClick : DashboardUiEvent
    data class CheckoutStudentClick(val aulaId: Long) : DashboardUiEvent
    data class AddExtraTimeClick(val aulaId: Long, val minutes: Int) : DashboardUiEvent
    object RefreshTimerOnResume : DashboardUiEvent
}
