package com.example.core.preferences

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("valida_moto_prefs", Context.MODE_PRIVATE)

    // Settings
    var defaultDuration: Int
        get() = prefs.getInt("default_duration", 50)
        set(value) = prefs.edit().putInt("default_duration", value).apply()

    var isBeepEnabled: Boolean
        get() = prefs.getBoolean("is_beep_enabled", true)
        set(value) = prefs.edit().putBoolean("is_beep_enabled", value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean("is_vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("is_vibration_enabled", value).apply()

    // Instructor info ID cache and credential fields
    var instructorId: Long
        get() = prefs.getLong("instructor_id", -1L)
        set(value) = prefs.edit().putLong("instructor_id", value).apply()

    var instructorCpf: String
        get() = prefs.getString("instructor_cpf", "") ?: ""
        set(value) = prefs.edit().putString("instructor_cpf", value).apply()

    var instructorNumRegistro: String
        get() = prefs.getString("instructor_num_registro", "") ?: ""
        set(value) = prefs.edit().putString("instructor_num_registro", value).apply()

    var instructorCategoria: String
        get() = prefs.getString("instructor_categoria", "") ?: ""
        set(value) = prefs.edit().putString("instructor_categoria", value).apply()

    var instructorUf: String
        get() = prefs.getString("instructor_uf", "") ?: ""
        set(value) = prefs.edit().putString("instructor_uf", value).apply()

    var instructorEmissao: String
        get() = prefs.getString("instructor_emissao", "") ?: ""
        set(value) = prefs.edit().putString("instructor_emissao", value).apply()

    var instructorPdfPath: String
        get() = prefs.getString("instructor_pdf_path", "") ?: ""
        set(value) = prefs.edit().putString("instructor_pdf_path", value).apply()

    var instructorPdfName: String
        get() = prefs.getString("instructor_pdf_name", "") ?: ""
        set(value) = prefs.edit().putString("instructor_pdf_name", value).apply()

    // Active Class State (State Machine Recovery)
    var activeStep: Int
        get() = prefs.getInt("active_step", 0) // 0 = no class active, 1..8 = active steps
        set(value) = prefs.edit().putInt("active_step", value).apply()

    var activeAulaId: Long
        get() = prefs.getLong("active_aula_id", -1L)
        set(value) = prefs.edit().putLong("active_aula_id", value).apply()

    var activeAlunoId: Long
        get() = prefs.getLong("active_aluno_id", -1L)
        set(value) = prefs.edit().putLong("active_aluno_id", value).apply()

    var activeMotoId: Long
        get() = prefs.getLong("active_moto_id", -1L)
        set(value) = prefs.edit().putLong("active_moto_id", value).apply()

    var activeKmInicial: Int
        get() = prefs.getInt("active_km_inicial", 0)
        set(value) = prefs.edit().putInt("active_km_inicial", value).apply()

    var activeFotoPainelInicio: String?
        get() = prefs.getString("active_foto_painel_inicio", null)
        set(value) = prefs.edit().putString("active_foto_painel_inicio", value).apply()

    var activeStartTime: Long
        get() = prefs.getLong("active_start_time", 0L)
        set(value) = prefs.edit().putLong("active_start_time", value).apply()

    var activeDurationMinutes: Int
        get() = prefs.getInt("active_duration_minutes", 50)
        set(value) = prefs.edit().putInt("active_duration_minutes", value).apply()

    var instructorPoseIndex: Int
        get() = prefs.getInt("instructor_pose_index", 0)
        set(value) = prefs.edit().putInt("instructor_pose_index", value).apply()

    var studentPoseIndex: Int
        get() = prefs.getInt("student_pose_index", 0)
        set(value) = prefs.edit().putInt("student_pose_index", value).apply()

    var finalInstructorPoseIndex: Int
        get() = prefs.getInt("final_instructor_pose_index", 0)
        set(value) = prefs.edit().putInt("final_instructor_pose_index", value).apply()

    var finalStudentPoseIndex: Int
        get() = prefs.getInt("final_student_pose_index", 0)
        set(value) = prefs.edit().putInt("final_student_pose_index", value).apply()

    // Add Student Flow Cache
    var isAddingStudent: Boolean
        get() = prefs.getBoolean("is_adding_student", false)
        set(value) = prefs.edit().putBoolean("is_adding_student", value).apply()

    var addStudentStep: Int
        get() = prefs.getInt("add_student_step", 1)
        set(value) = prefs.edit().putInt("add_student_step", value).apply()

    var addStudentSelectedId: Long
        get() = prefs.getLong("add_student_selected_id", -1L)
        set(value) = prefs.edit().putLong("add_student_selected_id", value).apply()

    var addStudentSelectedMotoId: Long
        get() = prefs.getLong("add_student_selected_moto_id", -1L)
        set(value) = prefs.edit().putLong("add_student_selected_moto_id", value).apply()

    var addStudentKmInicial: String
        get() = prefs.getString("add_student_km_inicial", "") ?: ""
        set(value) = prefs.edit().putString("add_student_km_inicial", value).apply()

    var addStudentFotoPainel: String?
        get() = prefs.getString("add_student_foto_painel", null)
        set(value) = prefs.edit().putString("add_student_foto_painel", value).apply()

    var addStudentPoseIndex: Int
        get() = prefs.getInt("add_student_pose_index", 0)
        set(value) = prefs.edit().putInt("add_student_pose_index", value).apply()

    var addStudentAgendamentoId: Long
        get() = prefs.getLong("add_student_agendamento_id", 0L)
        set(value) = prefs.edit().putLong("add_student_agendamento_id", value).apply()

    var addStudentAgendamentoDuration: Int
        get() = prefs.getInt("add_student_agendamento_duration", 0)
        set(value) = prefs.edit().putInt("add_student_agendamento_duration", value).apply()

    // Google Backup and Sync State
    var googleAccountName: String?
        get() = prefs.getString("google_account_name", null)
        set(value) = prefs.edit().putString("google_account_name", value).apply()

    var googleAccountEmail: String?
        get() = prefs.getString("google_account_email", null)
        set(value) = prefs.edit().putString("google_account_email", value).apply()

    var googleProfilePic: String?
        get() = prefs.getString("google_profile_pic", null)
        set(value) = prefs.edit().putString("google_profile_pic", value).apply()

    var googleLastSyncTime: String?
        get() = prefs.getString("google_last_sync_time", null)
        set(value) = prefs.edit().putString("google_last_sync_time", value).apply()

    var isGoogleBackupEnabled: Boolean
        get() = prefs.getBoolean("is_google_backup_enabled", false)
        set(value) = prefs.edit().putBoolean("is_google_backup_enabled", value).apply()

    var googleBackupFrequency: String
        get() = prefs.getString("google_backup_frequency", "Diário") ?: "Diário"
        set(value) = prefs.edit().putString("google_backup_frequency", value).apply()

    var hasPendingSync: Boolean
        get() = prefs.getBoolean("has_pending_sync", false)
        set(value) = prefs.edit().putBoolean("has_pending_sync", value).apply()

    var activeSessionId: String?
        get() = prefs.getString("active_session_id", null)
        set(value) = prefs.edit().putString("active_session_id", value).apply()

    var customLogoPath: String?
        get() = prefs.getString("custom_logo_path", null)
        set(value) = prefs.edit().putString("custom_logo_path", value).apply()

    var customDurations: String
        get() = prefs.getString("custom_durations", "") ?: ""
        set(value) = prefs.edit().putString("custom_durations", value).apply()

    var customGoogleAccounts: String
        get() = prefs.getString("custom_google_accounts", "") ?: ""
        set(value) = prefs.edit().putString("custom_google_accounts", value).apply()

    // Clear active class state
    fun clearActiveClassState() {
        prefs.edit()
            .putInt("active_step", 0)
            .putLong("active_aula_id", -1L)
            .putLong("active_aluno_id", -1L)
            .putLong("active_moto_id", -1L)
            .putInt("active_km_inicial", 0)
            .putString("active_foto_painel_inicio", null)
            .putLong("active_start_time", 0L)
            .putInt("active_duration_minutes", 50)
            .putInt("instructor_pose_index", 0)
            .putInt("student_pose_index", 0)
            .putInt("final_instructor_pose_index", 0)
            .putInt("final_student_pose_index", 0)
            .putBoolean("is_adding_student", false)
            .putInt("add_student_step", 1)
            .putLong("add_student_selected_id", -1L)
            .putLong("add_student_selected_moto_id", -1L)
            .putString("add_student_km_inicial", "")
            .putString("add_student_foto_painel", null)
            .putInt("add_student_pose_index", 0)
            .putString("active_session_id", null)
            .apply()
    }
}
