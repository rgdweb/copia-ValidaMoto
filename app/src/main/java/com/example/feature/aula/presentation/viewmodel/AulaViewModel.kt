package com.example.feature.aula.presentation.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.Context
import android.widget.Toast
import com.example.util.FileHelper
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.database.entity.*
import com.example.core.database.dao.AulaWithDetails
import com.example.core.preferences.AppPreferences
import com.example.core.backup.BackupHelper
import com.example.data.GoogleDriveService
import com.example.data.SessionRecoveryManager
import com.example.feature.aula.data.repository.AulaRepositoryImpl
import com.example.feature.aula.domain.repository.AulaRepository
import com.example.feature.aula.domain.usecases.*
import com.example.util.PdfReportGenerator
import com.example.util.SoundAndVibrationHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AulaViewModel(
    application: Application,
    private val getAulasWithDetailsUseCase: GetAulasWithDetailsUseCase,
    private val getAlunosUseCase: GetAlunosUseCase,
    private val getMotosUseCase: GetMotosUseCase,
    private val getInstrutorUseCase: GetInstrutorUseCase,
    private val insertAulaUseCase: InsertAulaUseCase,
    private val updateAulaUseCase: UpdateAulaUseCase,
    private val deleteAulaUseCase: DeleteAulaUseCase,
    private val updateAlunoUseCase: UpdateAlunoUseCase,
    private val updateMotoUseCase: UpdateMotoUseCase,
    private val insertAulaFotoUseCase: InsertAulaFotoUseCase,
    private val insertAllAulaFotosUseCase: InsertAllAulaFotosUseCase,
    private val insertEventoLogUseCase: InsertEventoLogUseCase,
    private val repository: AulaRepository
) : AndroidViewModel(application) {

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getDatabase(application)
            val repo = AulaRepositoryImpl(db)
            return AulaViewModel(
                application = application,
                getAulasWithDetailsUseCase = GetAulasWithDetailsUseCase(repo),
                getAlunosUseCase = GetAlunosUseCase(repo),
                getMotosUseCase = GetMotosUseCase(repo),
                getInstrutorUseCase = GetInstrutorUseCase(repo),
                insertAulaUseCase = InsertAulaUseCase(repo),
                updateAulaUseCase = UpdateAulaUseCase(repo),
                deleteAulaUseCase = DeleteAulaUseCase(repo),
                updateAlunoUseCase = UpdateAlunoUseCase(repo),
                updateMotoUseCase = UpdateMotoUseCase(repo),
                insertAulaFotoUseCase = InsertAulaFotoUseCase(repo),
                insertAllAulaFotosUseCase = InsertAllAulaFotosUseCase(repo),
                insertEventoLogUseCase = InsertEventoLogUseCase(repo),
                repository = repo
            ) as T
        }
    }

    private val CHANNEL_ID = "aula_notification_channel"
    val prefs: AppPreferences = AppPreferences(application)
    private val soundVib: SoundAndVibrationHelper = SoundAndVibrationHelper(application)
    val googleDriveService: GoogleDriveService = GoogleDriveService(application)
    private val sessionRecoveryManager: SessionRecoveryManager = SessionRecoveryManager(application)
    private val backupHelper = BackupHelper(
        context = application,
        getAllAulasCount = { allAulas.value.size },
        getAllAlunosCount = { allAlunos.value.size },
        auditLog = { tipo, desc -> auditLog(tipo, desc) }
    )

    // State Machine States
    private val _activeStep = MutableStateFlow(prefs.activeStep)
    val activeStep: StateFlow<Int> = _activeStep.asStateFlow()

    private val _activeAulaId = MutableStateFlow(prefs.activeAulaId)
    val activeAulaId: StateFlow<Long> = _activeAulaId.asStateFlow()

    private val _selectedAlunoId = MutableStateFlow(prefs.activeAlunoId)
    val selectedAlunoId: StateFlow<Long> = _selectedAlunoId.asStateFlow()

    private val _selectedMotoId = MutableStateFlow(prefs.activeMotoId)
    val selectedMotoId: StateFlow<Long> = _selectedMotoId.asStateFlow()

    private val _kmInicialInput = MutableStateFlow(if (prefs.activeKmInicial > 0) prefs.activeKmInicial.toString() else "")
    val kmInicialInput: StateFlow<String> = _kmInicialInput.asStateFlow()

    private val _kmFinalInput = MutableStateFlow("")
    val kmFinalInput: StateFlow<String> = _kmFinalInput.asStateFlow()

    private val _fotoPainelInicio = MutableStateFlow<String?>(prefs.activeFotoPainelInicio)
    val fotoPainelInicio: StateFlow<String?> = _fotoPainelInicio.asStateFlow()

    private val _fotoPainelFim = MutableStateFlow<String?>(null)
    val fotoPainelFim: StateFlow<String?> = _fotoPainelFim.asStateFlow()

    private val _instructorPoseIndex = MutableStateFlow(prefs.instructorPoseIndex)
    val instructorPoseIndex: StateFlow<Int> = _instructorPoseIndex.asStateFlow()

    private val _studentPoseIndex = MutableStateFlow(prefs.studentPoseIndex)
    val studentPoseIndex: StateFlow<Int> = _studentPoseIndex.asStateFlow()

    private val _finalInstructorPoseIndex = MutableStateFlow(prefs.finalInstructorPoseIndex)
    val finalInstructorPoseIndex: StateFlow<Int> = _finalInstructorPoseIndex.asStateFlow()

    private val _finalStudentPoseIndex = MutableStateFlow(prefs.finalStudentPoseIndex)
    val finalStudentPoseIndex: StateFlow<Int> = _finalStudentPoseIndex.asStateFlow()

    private val _timeLeftSeconds = MutableStateFlow(0)
    val timeLeftSeconds: StateFlow<Int> = _timeLeftSeconds.asStateFlow()

    private val _isTimerPaused = MutableStateFlow(false)
    val isTimerPaused: StateFlow<Boolean> = _isTimerPaused.asStateFlow()

    private val _isCountUpMode = MutableStateFlow(false)
    val isCountUpMode: StateFlow<Boolean> = _isCountUpMode.asStateFlow()

    private val _isAddingStudent = MutableStateFlow(prefs.isAddingStudent)
    val isAddingStudent: StateFlow<Boolean> = _isAddingStudent.asStateFlow()

    // Add Student Wizard States
    private val _addStudentStep = MutableStateFlow(prefs.addStudentStep)
    val addStudentStep: StateFlow<Int> = _addStudentStep.asStateFlow()

    private val _addStudentSelectedId = MutableStateFlow(prefs.addStudentSelectedId)
    val addStudentSelectedId: StateFlow<Long> = _addStudentSelectedId.asStateFlow()

    private val _addStudentSelectedMotoId = MutableStateFlow(prefs.addStudentSelectedMotoId)
    val addStudentSelectedMotoId: StateFlow<Long> = _addStudentSelectedMotoId.asStateFlow()

    private val _addStudentKmInicial = MutableStateFlow(prefs.addStudentKmInicial)
    val addStudentKmInicial: StateFlow<String> = _addStudentKmInicial.asStateFlow()

    private val _addStudentFotoPainel = MutableStateFlow<String?>(prefs.addStudentFotoPainel)
    val addStudentFotoPainel: StateFlow<String?> = _addStudentFotoPainel.asStateFlow()

    private val _addStudentPoseIndex = MutableStateFlow(prefs.addStudentPoseIndex)
    val addStudentPoseIndex: StateFlow<Int> = _addStudentPoseIndex.asStateFlow()

    // Recovery State
    private val _showRecoveryDialog = MutableStateFlow(false)
    val showRecoveryDialog: StateFlow<Boolean> = _showRecoveryDialog.asStateFlow()

    private val _recoveryInstructor = MutableStateFlow("")
    val recoveryInstructor: StateFlow<String> = _recoveryInstructor.asStateFlow()

    private val _recoveryStartTime = MutableStateFlow("")
    val recoveryStartTime: StateFlow<String> = _recoveryStartTime.asStateFlow()

    private val _recoveryActiveCount = MutableStateFlow(0)
    val recoveryActiveCount: StateFlow<Int> = _recoveryActiveCount.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(prefs.googleLastSyncTime ?: "Nunca")
    val lastBackupTime: StateFlow<String> = _lastBackupTime.asStateFlow()

    fun updateLastBackupTime(time: String) {
        prefs.googleLastSyncTime = time
        _lastBackupTime.value = time
    }

    val pendingBackupCount: StateFlow<Int> by lazy {
        repository.getAulasWithDetailsFlow().map { list ->
            list.count { it.statusAula == "pendente" && it.dataHoraFim > 0L }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    }

    private val _customLogoPath = MutableStateFlow(prefs.customLogoPath)
    val customLogoPath: StateFlow<String?> = _customLogoPath.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    // Warning state
    data class LessonWarning(
        val aulaId: Long,
        val alunoNome: String,
        val minutesRemaining: Int,
        val type: String
    )

    private val _warningAlert = MutableStateFlow<LessonWarning?>(null)
    val warningAlert: StateFlow<LessonWarning?> = _warningAlert.asStateFlow()

    private val _timeUpAlertLesson = MutableStateFlow<AulaWithDetails?>(null)
    val timeUpAlertLesson: StateFlow<AulaWithDetails?> = _timeUpAlertLesson.asStateFlow()

    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMillis: StateFlow<Long> = _currentTimeMillis.asStateFlow()

    // Alert lists
    private val alerted10Min = mutableSetOf<Long>()
    private val alerted5Min = mutableSetOf<Long>()
    private val alerted1Min = mutableSetOf<Long>()
    private val alertedAulas = mutableSetOf<Long>()

    // Core flows mapped from domain
    val allAulas: StateFlow<List<AulaWithDetails>> = getAulasWithDetailsUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeLessons: StateFlow<List<AulaWithDetails>> = allAulas.map { list ->
        list.filter { it.dataHoraFim == 0L && it.statusAula == "pendente" }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentInstrutor: StateFlow<Instrutor?> = getInstrutorUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allAlunos: StateFlow<List<Aluno>> = getAlunosUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allMotos: StateFlow<List<Moto>> = getMotosUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedLessonForFlow = MutableStateFlow<AulaWithDetails?>(null)
    val selectedLessonForFlow: StateFlow<AulaWithDetails?> = _selectedLessonForFlow.asStateFlow()

    init {
        // Initialize Google Drive account if set
        val lastAccount = GoogleSignIn.getLastSignedInAccount(application)
        if (lastAccount != null) {
            googleDriveService.initializeWithAccount(lastAccount)
        }

        // Ticking loop for warnings and session state saves
        viewModelScope.launch {
            var syncCounter = 0
            while (true) {
                try {
                    delay(1000)
                    _currentTimeMillis.value = System.currentTimeMillis()
                    checkBeepsAndVibrations()
                    
                    syncCounter++
                    if (syncCounter >= 30) {
                        syncCounter = 0
                        saveAndUploadSessionState()
                    }
                } catch (e: Exception) {
                    Log.e("AulaViewModel", "Error in ticking loop", e)
                }
            }
        }

        // Check for session recovery on startup
        viewModelScope.launch {
            checkStateRecoveryOnStart()
        }

        // Reactive alarm scheduling for active lessons
        viewModelScope.launch {
            val activeIds = mutableSetOf<Long>()
            activeLessons.collect { lessons ->
                val currentIds = lessons.map { it.id }.toSet()
                // Cancel alarms for lessons that are no longer active
                val removedIds = activeIds - currentIds
                removedIds.forEach { id ->
                    cancelAulaAlarms(id)
                }
                activeIds.clear()
                activeIds.addAll(currentIds)
                
                // Schedule alarms for currently active lessons
                lessons.forEach { aula ->
                    scheduleAulaAlarms(aula)
                }
            }
        }
    }

    private suspend fun checkStateRecoveryOnStart() {
        val savedStep = prefs.activeStep
        if (savedStep == 0 && prefs.isGoogleBackupEnabled && !prefs.googleAccountEmail.isNullOrEmpty() && googleDriveService.isInitialized()) {
            val localTempFile = sessionRecoveryManager.getTempFileForUpload()
            val hasCloudFile = withContext(Dispatchers.IO) {
                googleDriveService.downloadSessionRecovery(localTempFile)
            }
            if (hasCloudFile) {
                val cloudState = withContext(Dispatchers.IO) {
                    sessionRecoveryManager.loadSessionState()
                }
                if (cloudState != null) {
                    val lastUpdate = cloudState.optLong("lastUpdate", 0L)
                    val activeSessionId = cloudState.optString("sessionId", "")
                    val activeList = repository.getAulasWithDetailsFlow().first().filter { it.dataHoraFim == 0L && it.statusAula == "pendente" }
                    if (activeList.isNotEmpty() && !activeSessionId.isNullOrEmpty() && lastUpdate > 0) {
                        val instObj = cloudState.optJSONObject("instrutor")
                        val instructorName = instObj?.optString("nome", "Instrutor") ?: "Instrutor"
                        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(lastUpdate))
                        
                        _recoveryInstructor.value = instructorName
                        _recoveryStartTime.value = dateStr
                        _recoveryActiveCount.value = activeList.size
                        
                        _showRecoveryDialog.value = true
                        return
                    }
                }
            }
        }

        // Standard local recovery
        if (savedStep in 1..7) {
            val activeList = repository.getAulasWithDetailsFlow().first().filter { it.dataHoraFim == 0L && it.statusAula == "pendente" }
            val activeCount = activeList.size
            if (activeCount > 0) {
                val inst = repository.getInstrutor()
                val instructorName = inst?.nome ?: "Instrutor"
                val startTime = activeList.minOfOrNull { it.dataHoraInicio } ?: System.currentTimeMillis()
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(startTime))
                
                _recoveryInstructor.value = instructorName
                _recoveryStartTime.value = dateStr
                _recoveryActiveCount.value = activeCount
                
                _showRecoveryDialog.value = true
            }
        }
    }

    fun restoreActiveClass() {
        _showRecoveryDialog.value = false
        _activeStep.value = prefs.activeStep
        _activeAulaId.value = prefs.activeAulaId
        _selectedAlunoId.value = prefs.activeAlunoId
        _selectedMotoId.value = prefs.activeMotoId
        _kmInicialInput.value = if (prefs.activeKmInicial > 0) prefs.activeKmInicial.toString() else ""
        _fotoPainelInicio.value = prefs.activeFotoPainelInicio
        _instructorPoseIndex.value = prefs.instructorPoseIndex
        _studentPoseIndex.value = prefs.studentPoseIndex
        _finalInstructorPoseIndex.value = prefs.finalInstructorPoseIndex
        _finalStudentPoseIndex.value = prefs.finalStudentPoseIndex

        if (prefs.activeStep in listOf(1, 2, 3, 5, 6, 7)) {
            viewModelScope.launch {
                val lessonId = prefs.activeAulaId
                if (lessonId != -1L) {
                    val lesson = repository.getAulaWithDetailsById(lessonId)
                    if (lesson != null) {
                        selectLessonForFlow(lesson)
                    }
                }
            }
        }

        if (prefs.activeStep == 4) {
            val startTime = prefs.activeStartTime
            val totalDurationSeconds = prefs.activeDurationMinutes * 60
            val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000

            if (elapsedSeconds >= totalDurationSeconds) {
                viewModelScope.launch {
                    advanceToStep(5)
                }
            } else {
                _timeLeftSeconds.value = (totalDurationSeconds - elapsedSeconds).toInt()
            }
        }
    }

    fun cancelRecoveryDialog() {
        _showRecoveryDialog.value = false
    }

    fun finalizeSessionFromRecovery() {
        _showRecoveryDialog.value = false
        viewModelScope.launch {
            val activeList = repository.getAulasWithDetailsFlow().first().filter { it.dataHoraFim == 0L && it.statusAula == "pendente" }
            for (aulaWithDetails in activeList) {
                val aula = repository.getAulaById(aulaWithDetails.id)
                if (aula != null) {
                    val updated = aula.copy(
                        dataHoraFim = System.currentTimeMillis(),
                        statusAula = "cancelada"
                    )
                    repository.updateAula(updated)
                    auditLog("aula_cancelada", "Aula ${aula.id} cancelada na finalização em lote da recuperação", alunoId = aula.alunoId, alunoNome = aulaWithDetails.alunoNome, motoId = aula.motoId, motoModelo = aulaWithDetails.motoModelo)
                }
            }
            
            if (prefs.isGoogleBackupEnabled && googleDriveService.isInitialized()) {
                googleDriveService.deleteSessionRecovery()
            }
            sessionRecoveryManager.deleteSessionState()
            
            prefs.clearActiveClassState()
            clearActiveStateFlows()
            auditLog("sessao_encerrada", "Sessão finalizada a partir da tela de recuperação. Aulas pendentes foram canceladas.")
        }
    }

    fun selectLessonForFlow(lesson: AulaWithDetails) {
        _selectedLessonForFlow.value = lesson
        _activeAulaId.value = lesson.id
        _selectedAlunoId.value = lesson.alunoId
        _selectedMotoId.value = lesson.motoId
        _kmInicialInput.value = lesson.kmInicial.toString()
        _kmFinalInput.value = lesson.kmFinal.toString()
        _fotoPainelInicio.value = lesson.fotoPainelInicio
        _fotoPainelFim.value = lesson.fotoPainelFim

        viewModelScope.launch {
            val calculatedProgress = calculateNextPoseIndexFromDatabase(lesson.id, lesson.etapa)
            val updatedLesson = repository.getAulaById(lesson.id)
            if (updatedLesson != null && updatedLesson.progressoEtapa != calculatedProgress) {
                repository.updateAula(updatedLesson.copy(progressoEtapa = calculatedProgress))
                val fresh = repository.getAulaWithDetailsById(lesson.id)
                if (fresh != null) {
                    _selectedLessonForFlow.value = fresh
                }
            }

            val currentProgress = _selectedLessonForFlow.value?.progressoEtapa ?: calculatedProgress
            _instructorPoseIndex.value = if (lesson.etapa == 1) currentProgress else 0
            _studentPoseIndex.value = if (lesson.etapa == 2) currentProgress else 0
            _finalInstructorPoseIndex.value = if (lesson.etapa == 5) currentProgress else 0
            _finalStudentPoseIndex.value = if (lesson.etapa == 6) currentProgress else 0

            restorePhotosFromDatabaseToPrefs(lesson.id)
        }
    }

    private suspend fun restorePhotosFromDatabaseToPrefs(aulaId: Long) = withContext(Dispatchers.IO) {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("valida_moto_prefs", Context.MODE_PRIVATE)
        val edit = sharedPrefs.edit()
        val existingFotos = repository.getFotosForAula(aulaId)
        existingFotos.forEach { foto ->
            val key = "photo_${aulaId}_${foto.tipo}_${foto.pose}"
            edit.putString(key, foto.caminhoFoto)
        }
        edit.commit()
    }

    private suspend fun calculateNextPoseIndexFromDatabase(aulaId: Long, etapa: Int): Int = withContext(Dispatchers.IO) {
        val targetTipo = when (etapa) {
            1 -> "instrutor_inicio"
            2 -> "aluno_inicio"
            5 -> "instrutor_fim"
            6 -> "aluno_fim"
            else -> return@withContext 0
        }
        val poses = listOf("direita")
        val existingFotos = repository.getFotosForAula(aulaId).filter { it.tipo == targetTipo }
        var nextIndex = 0
        for (i in poses.indices) {
            val poseName = poses[i]
            if (existingFotos.any { it.pose == poseName && !it.caminhoFoto.isNullOrBlank() }) {
                nextIndex = i + 1
            } else {
                break
            }
        }
        nextIndex
    }

    fun clearSelectedLessonForFlow() {
        _selectedLessonForFlow.value = null
        clearActiveStateFlows()
    }

    suspend fun refreshSelectedLesson() {
        val current = _selectedLessonForFlow.value ?: return
        val fresh = repository.getAulaWithDetailsById(current.id)
        if (fresh != null) {
            _selectedLessonForFlow.value = fresh
            _activeAulaId.value = fresh.id
            _selectedAlunoId.value = fresh.alunoId
            _selectedMotoId.value = fresh.motoId
            _kmInicialInput.value = fresh.kmInicial.toString()
            _kmFinalInput.value = fresh.kmFinal.toString()
            _fotoPainelInicio.value = fresh.fotoPainelInicio
            _fotoPainelFim.value = fresh.fotoPainelFim
            _instructorPoseIndex.value = if (fresh.etapa == 1) fresh.progressoEtapa else 0
            _studentPoseIndex.value = if (fresh.etapa == 2) fresh.progressoEtapa else 0
            _finalInstructorPoseIndex.value = if (fresh.etapa == 5) fresh.progressoEtapa else 0
            _finalStudentPoseIndex.value = if (fresh.etapa == 6) fresh.progressoEtapa else 0
        }
    }

    fun confirmAndCreateLesson(alunoId: Long, motoId: Long) {
        viewModelScope.launch {
            val inst = repository.getInstrutor() ?: return@launch
            val moto = repository.getMotoById(motoId) ?: return@launch
            val pastConfirmed = repository.getCountAulasConfirmadas(alunoId)

            val aula = Aula(
                alunoId = alunoId,
                instrutorId = inst.id,
                motoId = motoId,
                dataHoraInicio = System.currentTimeMillis(),
                dataHoraFim = 0L,
                duracaoMinutos = prefs.defaultDuration,
                kmInicial = moto.kmAtual,
                kmFinal = 0,
                kmPercorrido = 0,
                fotoPainelInicio = "",
                fotoPainelFim = "",
                observacoes = "",
                statusAula = "pendente",
                aulasConfirmadasAteEntao = pastConfirmed + 1,
                etapa = 1,
                progressoEtapa = 0
            )

            val id = repository.insertAula(aula)
            val insertedAula = repository.getAulaWithDetailsById(id)
            if (insertedAula != null) {
                selectLessonForFlow(insertedAula)
            }
            _isAddingStudent.value = false
            prefs.isAddingStudent = false
        }
    }

    private fun clearActiveStateFlows() {
        _activeStep.value = 0
        _activeAulaId.value = -1L
        _selectedAlunoId.value = -1L
        _selectedMotoId.value = -1L
        _kmInicialInput.value = ""
        _kmFinalInput.value = ""
        _fotoPainelInicio.value = null
        _fotoPainelFim.value = null
        _instructorPoseIndex.value = 0
        _studentPoseIndex.value = 0
        _finalInstructorPoseIndex.value = 0
        _finalStudentPoseIndex.value = 0
        _timeLeftSeconds.value = 0
        _isTimerPaused.value = false
    }

    fun startNewSession() {
        viewModelScope.launch {
            prefs.clearActiveClassState()
            clearActiveStateFlows()
            
            val sessionId = UUID.randomUUID().toString()
            prefs.activeSessionId = sessionId
            
            _activeStep.value = 4
            prefs.activeStep = 4
        }
    }

    private suspend fun advanceToStep(step: Int) {
        val lesson = _selectedLessonForFlow.value ?: return
        val lessonId = lesson.id
        val updated = repository.getAulaById(lessonId)?.copy(etapa = step, progressoEtapa = 0)
        if (updated != null) {
            repository.updateAula(updated)
            refreshSelectedLesson()
        }
    }

    fun handlePosePhoto(tipo: String, pose: String, photoPath: String) {
        viewModelScope.launch {
            val lesson = _selectedLessonForFlow.value ?: return@launch
            val lessonId = lesson.id

            val key = "photo_${lessonId}_${tipo}_${pose}"
            val sharedPrefs = getApplication<Application>().getSharedPreferences("valida_moto_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString(key, photoPath).commit()

            try {
                repository.runInTransaction {
                    val existingFotos = repository.getFotosForAula(lessonId)
                    val existingFoto = existingFotos.find { it.tipo == tipo && it.pose == pose }
                    val aulaFoto = AulaFoto(
                        id = existingFoto?.id ?: 0,
                        aulaId = lessonId,
                        tipo = tipo,
                        pose = pose,
                        caminhoFoto = photoPath,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertAulaFoto(aulaFoto)

                    val freshLesson = repository.getAulaById(lessonId)
                    if (freshLesson != null) {
                        val currentStep = freshLesson.etapa
                        val nextIndex = freshLesson.progressoEtapa + 1

                        val updated = when (currentStep) {
                            1 -> {
                                if (nextIndex >= 1) {
                                    freshLesson.copy(etapa = 2, progressoEtapa = 0)
                                } else {
                                    freshLesson.copy(progressoEtapa = nextIndex)
                                }
                            }
                            2 -> {
                                if (nextIndex >= 1) {
                                    freshLesson.copy(etapa = 3, progressoEtapa = 0)
                                } else {
                                    freshLesson.copy(progressoEtapa = nextIndex)
                                }
                            }
                            5 -> {
                                if (nextIndex >= 1) {
                                    freshLesson.copy(etapa = 6, progressoEtapa = 0)
                                } else {
                                    freshLesson.copy(progressoEtapa = nextIndex)
                                }
                            }
                            6 -> {
                                if (nextIndex >= 1) {
                                    freshLesson.copy(etapa = 7, progressoEtapa = 0)
                                } else {
                                    freshLesson.copy(progressoEtapa = nextIndex)
                                }
                            }
                            else -> null
                        }

                        if (updated != null) {
                            repository.updateAula(updated)
                        }
                    }
                }
                refreshSelectedLesson()
            } catch (e: Exception) {
                sharedPrefs.edit().remove(key).commit()
                e.printStackTrace()
            }
        }
    }

    fun repeatPosePhoto() {
        // Redo current pose (no-op, index is kept)
    }

    fun selectStudent(alunoId: Long) {
        _selectedAlunoId.value = alunoId
        prefs.activeAlunoId = alunoId
    }

    fun selectMoto(motoId: Long) {
        _selectedMotoId.value = motoId
        prefs.activeMotoId = motoId
        viewModelScope.launch {
            val moto = repository.getMotoById(motoId)
            if (moto != null) {
                _kmInicialInput.value = moto.kmAtual.toString()
                prefs.activeKmInicial = moto.kmAtual
            }
        }
    }

    fun setKmInicial(kmStr: String) {
        val cleanStr = kmStr.filter { it.isDigit() }
        _kmInicialInput.value = cleanStr
        cleanStr.toIntOrNull()?.let {
            prefs.activeKmInicial = it
            viewModelScope.launch {
                val lesson = _selectedLessonForFlow.value
                if (lesson != null) {
                    val updated = repository.getAulaById(lesson.id)?.copy(kmInicial = it)
                    if (updated != null) {
                        repository.updateAula(updated)
                        refreshSelectedLesson()
                    }
                }
            }
        }
    }

    fun setKmFinal(kmStr: String) {
        val cleanStr = kmStr.filter { it.isDigit() }
        _kmFinalInput.value = cleanStr
        cleanStr.toIntOrNull()?.let {
            viewModelScope.launch {
                val lesson = _selectedLessonForFlow.value
                if (lesson != null) {
                    val updated = repository.getAulaById(lesson.id)?.copy(kmFinal = it)
                    if (updated != null) {
                        repository.updateAula(updated)
                        refreshSelectedLesson()
                    }
                }
            }
        }
    }

    fun setFotoPainelInicio(path: String) {
        if (path.isNotEmpty()) {
            FileHelper.normalizeFileOrientation(File(path))
        }
        _fotoPainelInicio.value = path
        prefs.activeFotoPainelInicio = path
        viewModelScope.launch {
            val lesson = _selectedLessonForFlow.value
            if (lesson != null) {
                val updated = repository.getAulaById(lesson.id)?.copy(fotoPainelInicio = path)
                if (updated != null) {
                    repository.updateAula(updated)
                    refreshSelectedLesson()
                }
            }
        }
    }

    fun setFotoPainelFim(path: String) {
        if (path.isNotEmpty()) {
            FileHelper.normalizeFileOrientation(File(path))
        }
        _fotoPainelFim.value = path
        viewModelScope.launch {
            val lesson = _selectedLessonForFlow.value
            if (lesson != null) {
                val updated = repository.getAulaById(lesson.id)?.copy(fotoPainelFim = path)
                if (updated != null) {
                    repository.updateAula(updated)
                    refreshSelectedLesson()
                }
            }
        }
    }

    fun confirmAndStartClass() {
        viewModelScope.launch {
            val lesson = _selectedLessonForFlow.value ?: return@launch
            val lessonId = lesson.id
            val kmIni = _kmInicialInput.value.filter { it.isDigit() }.toIntOrNull() ?: 0
            val pImage = _fotoPainelInicio.value ?: ""

            if (kmIni == 0 || pImage.isEmpty()) {
                return@launch
            }

            saveAllPosePhotosToDatabase(lessonId, "inicio")

            val updated = repository.getAulaById(lessonId)?.copy(
                dataHoraInicio = System.currentTimeMillis(),
                kmInicial = kmIni,
                fotoPainelInicio = pImage,
                etapa = 4,
                progressoEtapa = 0
            )
            if (updated != null) {
                repository.updateAula(updated)
            }

            clearSelectedLessonForFlow()

            soundVib.playBeep(400, force = true)
            
            saveAndUploadSessionState()
        }
    }

    private suspend fun saveAllPosePhotosToDatabase(aulaId: Long, stage: String) {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("valida_moto_prefs", Context.MODE_PRIVATE)
        val list = mutableListOf<AulaFoto>()
        val existingFotos = repository.getFotosForAula(aulaId)

        if (stage == "inicio") {
            listOf("direita").forEach { pose ->
                val instPath = sharedPrefs.getString("photo_${aulaId}_instrutor_inicio_$pose", null)
                val instExisting = existingFotos.find { it.tipo == "instrutor_inicio" && it.pose == pose }
                if (instPath != null) {
                    list.add(AulaFoto(id = instExisting?.id ?: 0, aulaId = aulaId, tipo = "instrutor_inicio", pose = pose, caminhoFoto = instPath, timestamp = System.currentTimeMillis()))
                } else if (instExisting != null) {
                    list.add(instExisting)
                }

                val alPath = sharedPrefs.getString("photo_${aulaId}_aluno_inicio_$pose", null)
                val alExisting = existingFotos.find { it.tipo == "aluno_inicio" && it.pose == pose }
                if (alPath != null) {
                    list.add(AulaFoto(id = alExisting?.id ?: 0, aulaId = aulaId, tipo = "aluno_inicio", pose = pose, caminhoFoto = alPath, timestamp = System.currentTimeMillis()))
                } else if (alExisting != null) {
                    list.add(alExisting)
                }
            }
        } else {
            listOf("direita").forEach { pose ->
                val instPath = sharedPrefs.getString("photo_${aulaId}_instrutor_fim_$pose", null)
                val instExisting = existingFotos.find { it.tipo == "instrutor_fim" && it.pose == pose }
                if (instPath != null) {
                    list.add(AulaFoto(id = instExisting?.id ?: 0, aulaId = aulaId, tipo = "instrutor_fim", pose = pose, caminhoFoto = instPath, timestamp = System.currentTimeMillis()))
                } else if (instExisting != null) {
                    list.add(instExisting)
                }

                val alPath = sharedPrefs.getString("photo_${aulaId}_aluno_fim_$pose", null)
                val alExisting = existingFotos.find { it.tipo == "aluno_fim" && it.pose == pose }
                if (alPath != null) {
                    list.add(AulaFoto(id = alExisting?.id ?: 0, aulaId = aulaId, tipo = "aluno_fim", pose = pose, caminhoFoto = alPath, timestamp = System.currentTimeMillis()))
                } else if (alExisting != null) {
                    list.add(alExisting)
                }
            }
        }
        if (list.isNotEmpty()) {
            repository.insertAllAulaFotos(list)
        }
    }

    fun toggleTimerPause() {
        _isTimerPaused.value = !_isTimerPaused.value
    }

    fun toggleCountUpMode() {
        _isCountUpMode.value = !_isCountUpMode.value
    }

    fun finishClassEarly() {
        viewModelScope.launch {
            val lesson = _selectedLessonForFlow.value ?: return@launch
            soundVib.playBeep(1500, force = true)
            advanceToStep(5)
        }
    }

    fun finalizeFinalKmAndClass() {
        viewModelScope.launch {
            val lesson = _selectedLessonForFlow.value ?: return@launch
            val lessonId = lesson.id
            val kmFin = _kmFinalInput.value.filter { it.isDigit() }.toIntOrNull() ?: 0
            val pFimImage = _fotoPainelFim.value ?: ""

            if (kmFin == 0 || pFimImage.isEmpty()) {
                return@launch
            }

            val aula = repository.getAulaById(lessonId) ?: return@launch
            if (kmFin < aula.kmInicial) {
                return@launch
            }
            val kmPercorrido = kmFin - aula.kmInicial

            repository.runInTransaction {
                saveAllPosePhotosToDatabase(lessonId, "fim")

                val updatedAula = aula.copy(
                    dataHoraFim = System.currentTimeMillis(),
                    kmFinal = kmFin,
                    kmPercorrido = if (kmPercorrido > 0) kmPercorrido else 0,
                    fotoPainelFim = pFimImage,
                    statusAula = "pendente",
                    etapa = 8,
                    progressoEtapa = 0
                )
                repository.updateAula(updatedAula)

                val al = repository.getAlunoById(aula.alunoId)
                if (al != null) {
                    val novasRealizadas = al.aulasRealizadas + 1
                    val novoStatus = if (novasRealizadas >= al.aulasContratadas) "Concluído" else al.status
                    repository.updateAluno(al.copy(aulasRealizadas = novasRealizadas, status = novoStatus))
                }

                val moto = repository.getMotoById(aula.motoId)
                if (moto != null && kmFin > moto.kmAtual) {
                    repository.updateMoto(moto.copy(kmAtual = kmFin))
                }

                if (aula.observacoes.contains("AGENDAMENTO_ID:")) {
                    val match = Regex("""AGENDAMENTO_ID:(\d+)""").find(aula.observacoes)
                    val agendamentoId = match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                    if (agendamentoId > 0L) {
                        val db = AppDatabase.getDatabase(getApplication())
                        val agendamento = db.agendamentoDao().getById(agendamentoId)
                        if (agendamento != null) {
                            val numAulasRegex = Regex("""\((\d+)\s*aula""")
                            val currentNumAulas = numAulasRegex.find(agendamento.observacoes)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                            val remainingAulas = currentNumAulas - 1
                            if (remainingAulas <= 0) {
                                db.agendamentoDao().update(agendamento.copy(status = "realizada"))
                                val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                                if (alarmManager != null) {
                                    val intent = Intent(getApplication(), com.example.feature.aula.presentation.receiver.AulaAlarmReceiver::class.java)
                                    val pendingIntent = PendingIntent.getBroadcast(
                                        getApplication(),
                                        (agendamentoId + 5000000L).toInt(),
                                        intent,
                                        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                                    )
                                    if (pendingIntent != null) {
                                        alarmManager.cancel(pendingIntent)
                                        pendingIntent.cancel()
                                    }
                                }
                            } else {
                                val updatedObs = agendamento.observacoes.replace(numAulasRegex, "($remainingAulas aula(s)")
                                db.agendamentoDao().update(agendamento.copy(observacoes = updatedObs))
                            }
                        }
                    }
                }
            }

            triggerSilentBackgroundSync()
            refreshSelectedLesson()
        }
    }

    fun generateAndOpenPdfForActiveSession() {
        viewModelScope.launch {
            val aulaId = _activeAulaId.value
            if (aulaId == -1L) return@launch
            val details = repository.getAulaWithDetailsById(aulaId) ?: return@launch
            val fotos = repository.getFotosForAula(aulaId)
            val generator = PdfReportGenerator(getApplication())
            val file = generator.generateReport(details, fotos)
            if (file != null) {
                generator.openPdf(file)
            }
        }
    }

    fun returnToSession() {
        _selectedLessonForFlow.value = null
        _activeStep.value = 4
        prefs.activeStep = 4
        
        _activeAulaId.value = -1L
        prefs.activeAulaId = -1L
        _selectedAlunoId.value = -1L
        prefs.activeAlunoId = -1L
        _selectedMotoId.value = -1L
        prefs.activeMotoId = -1L
        
        _kmFinalInput.value = ""
        _fotoPainelFim.value = null
    }

    // Student Addition (Active Flow inside Dashboard)
    fun startClassFromAgenda(alunoId: Long, agendamentoId: Long) {
        if (activeLessons.value.any { it.alunoId == alunoId }) {
            Toast.makeText(getApplication(), "Este aluno já possui uma aula em andamento.", Toast.LENGTH_LONG).show()
            return
        }

        startAddingStudent()
        if (!_isAddingStudent.value) return

        selectAddStudent(alunoId)

        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            val agendamento = db.agendamentoDao().getById(agendamentoId)
            val firstMotoInDb = repository.getMotosFlow().firstOrNull()?.firstOrNull()
            val motoId = agendamento?.motoId ?: (firstMotoInDb?.id ?: -1L)
            
            val timeRegex = Regex("""(\d{1,2}):(\d{2})\s*(?:às|as|-)\s*(\d{1,2}):(\d{2})""")
            val timeMatch = agendamento?.observacoes?.let { timeRegex.find(it) }
            val calculatedDuration = if (timeMatch != null) {
                val startH = timeMatch.groupValues[1].toInt()
                val startM = timeMatch.groupValues[2].toInt()
                val endH = timeMatch.groupValues[3].toInt()
                val endM = timeMatch.groupValues[4].toInt()
                val startTotalMin = startH * 60 + startM
                val endTotalMin = endH * 60 + endM
                val diff = endTotalMin - startTotalMin
                if (diff > 0) diff else null
            } else null

            val agendamentoDuration = calculatedDuration ?: run {
                val numAulasRegex = Regex("""\((\d+)\s*aula""")
                val currentNumAulas = agendamento?.observacoes?.let { numAulasRegex.find(it) }?.groupValues?.get(1)?.toIntOrNull() ?: 1
                (currentNumAulas * prefs.defaultDuration).coerceAtLeast(1)
            }

            if (motoId != -1L) {
                val moto = repository.getMotoById(motoId) ?: firstMotoInDb
                if (moto != null) {
                    selectAddMoto(moto.id, moto.kmAtual)
                }
            } else if (firstMotoInDb != null) {
                selectAddMoto(firstMotoInDb.id, firstMotoInDb.kmAtual)
            }
            prefs.addStudentAgendamentoId = agendamentoId
            prefs.addStudentAgendamentoDuration = agendamentoDuration
            advanceAddStudentStep(2)
        }
    }

    fun startAddingStudent() {
        _addStudentSelectedId.value = -1L
        _addStudentSelectedMotoId.value = -1L
        _addStudentKmInicial.value = ""
        _addStudentFotoPainel.value = null
        _addStudentPoseIndex.value = 0
        
        prefs.addStudentSelectedId = -1L
        prefs.addStudentSelectedMotoId = -1L
        prefs.addStudentKmInicial = ""
        prefs.addStudentFotoPainel = null
        prefs.addStudentPoseIndex = 0

        _addStudentStep.value = 1
        prefs.addStudentStep = 1
        _isAddingStudent.value = true
        prefs.isAddingStudent = true
    }

    fun cancelAddingStudent() {
        _isAddingStudent.value = false
        prefs.isAddingStudent = false
    }

    fun selectAddStudent(alunoId: Long) {
        _addStudentSelectedId.value = alunoId
        prefs.addStudentSelectedId = alunoId
    }

    fun advanceAddStudentStep(nextStep: Int) {
        _addStudentStep.value = nextStep
        prefs.addStudentStep = nextStep
    }

    fun handleAddStudentPosePhoto(photoPath: String) {
        viewModelScope.launch {
            if (photoPath.isNotEmpty()) {
                FileHelper.normalizeFileOrientation(File(photoPath))
            }
            val currentPoseIndex = _addStudentPoseIndex.value
            val poses = listOf("direita")
            val currentPose = poses[currentPoseIndex]
            val currentStep = _addStudentStep.value
            
            val key = if (currentStep == 2) "add_instructor_photo_$currentPose" else "add_student_photo_$currentPose"
            val sharedPrefs = getApplication<Application>().getSharedPreferences("valida_moto_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString(key, photoPath).commit()

            if (currentPoseIndex < poses.size - 1) {
                _addStudentPoseIndex.value = currentPoseIndex + 1
                prefs.addStudentPoseIndex = currentPoseIndex + 1
            } else {
                _addStudentPoseIndex.value = 0
                prefs.addStudentPoseIndex = 0
                if (currentStep == 2) {
                    advanceAddStudentStep(3)
                } else {
                    advanceAddStudentStep(4)
                }
            }
        }
    }

    fun repeatAddStudentPosePhoto() {
        // Keeps index same
    }

    fun selectAddMoto(motoId: Long, currentKm: Int) {
        _addStudentSelectedMotoId.value = motoId
        prefs.addStudentSelectedMotoId = motoId
        _addStudentKmInicial.value = currentKm.toString()
        prefs.addStudentKmInicial = currentKm.toString()
    }

    fun setAddStudentKmInicial(km: String) {
        _addStudentKmInicial.value = km
        prefs.addStudentKmInicial = km
    }

    fun setAddStudentFotoPainel(path: String) {
        if (path.isNotEmpty()) {
            FileHelper.normalizeFileOrientation(File(path))
        }
        _addStudentFotoPainel.value = path
        prefs.addStudentFotoPainel = path
    }

    fun confirmAndAddStudent() {
        viewModelScope.launch {
            val alunoId = _addStudentSelectedId.value
            val motoId = _addStudentSelectedMotoId.value
            val km = _addStudentKmInicial.value.toIntOrNull() ?: 0
            val painelPhoto = _addStudentFotoPainel.value ?: ""
            val inst = repository.getInstrutor()
            val instrutorId = inst?.id ?: prefs.instructorId

            if (alunoId == -1L || motoId == -1L || instrutorId == -1L) return@launch

            try {
                val agendamentoId = prefs.addStudentAgendamentoId
                val agendamentoDuration = prefs.addStudentAgendamentoDuration
                val obs = if (agendamentoId > 0L) "AGENDAMENTO_ID:$agendamentoId" else ""
                val durationToUse = prefs.defaultDuration
                
                prefs.addStudentAgendamentoId = 0L
                prefs.addStudentAgendamentoDuration = 0

                repository.runInTransaction {
                    val countConfirmadas = repository.getCountAulasConfirmadas(alunoId)
                    val aula = Aula(
                        alunoId = alunoId,
                        instrutorId = instrutorId,
                        motoId = motoId,
                        dataHoraInicio = System.currentTimeMillis(),
                        dataHoraFim = 0L,
                        duracaoMinutos = durationToUse,
                        kmInicial = km,
                        kmFinal = 0,
                        kmPercorrido = 0,
                        fotoPainelInicio = painelPhoto,
                        fotoPainelFim = "",
                        observacoes = obs,
                        statusAula = "pendente",
                        aulasConfirmadasAteEntao = countConfirmadas + 1
                    )

                    val aulaId = repository.insertAula(aula)

                    val poses = listOf("direita")
                    val sharedPrefs = getApplication<Application>().getSharedPreferences("valida_moto_prefs", Context.MODE_PRIVATE)
                    val fotosToInsert = mutableListOf<AulaFoto>()
                    
                    poses.forEach { pose ->
                        val cachedPath = sharedPrefs.getString("add_student_photo_$pose", "") ?: ""
                        if (cachedPath.isNotEmpty()) {
                            fotosToInsert.add(
                                AulaFoto(
                                    aulaId = aulaId,
                                    tipo = "aluno_inicio",
                                    pose = pose,
                                    caminhoFoto = cachedPath,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                        val instCachedPath = sharedPrefs.getString("add_instructor_photo_$pose", "") ?: ""
                        if (instCachedPath.isNotEmpty()) {
                            fotosToInsert.add(
                                AulaFoto(
                                    aulaId = aulaId,
                                    tipo = "instrutor_inicio",
                                    pose = pose,
                                    caminhoFoto = instCachedPath,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    repository.insertAllAulaFotos(fotosToInsert)

                    val edit = sharedPrefs.edit()
                    poses.forEach { pose ->
                        edit.remove("add_student_photo_$pose")
                        edit.remove("add_instructor_photo_$pose")
                    }
                    edit.commit()
                }

                _isAddingStudent.value = false
                prefs.isAddingStudent = false
                _addStudentStep.value = 1
                prefs.addStudentStep = 1
                _addStudentSelectedId.value = -1L
                _addStudentSelectedMotoId.value = -1L
                _addStudentKmInicial.value = ""
                _addStudentFotoPainel.value = null
                _addStudentPoseIndex.value = 0
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun checkoutStudent(aulaId: Long) {
        viewModelScope.launch {
            val lesson = repository.getAulaWithDetailsById(aulaId)
            val aula = repository.getAulaById(aulaId)
            if (lesson != null && aula != null) {
                val newEnd = if (aula.dataHoraFim == 0L) System.currentTimeMillis() else aula.dataHoraFim
                val updated = aula.copy(etapa = 5, progressoEtapa = 0, dataHoraFim = newEnd)
                repository.updateAula(updated)

                _activeAulaId.value = aulaId
                prefs.activeAulaId = aulaId
                _selectedAlunoId.value = lesson.alunoId
                prefs.activeAlunoId = lesson.alunoId
                _selectedMotoId.value = lesson.motoId
                prefs.activeMotoId = lesson.motoId
                
                _activeStep.value = 5
                prefs.activeStep = 5

                selectLessonForFlow(lesson.copy(etapa = 5, progressoEtapa = 0, dataHoraFim = newEnd))
            }
        }
    }

    fun startIndividualCheckout(aulaId: Long) {
        viewModelScope.launch {
            val details = repository.getAulaWithDetailsById(aulaId)
            val aula = repository.getAulaById(aulaId)
            if (details != null && aula != null) {
                val sharedPrefs = getApplication<Application>().getSharedPreferences("valida_moto_prefs", Context.MODE_PRIVATE)
                val edit = sharedPrefs.edit()
                listOf("direita").forEach { pose ->
                    edit.remove("photo_${aulaId}_instrutor_fim_$pose")
                    edit.remove("photo_${aulaId}_aluno_fim_$pose")
                }
                edit.apply()

                val newEnd = if (aula.dataHoraFim == 0L) System.currentTimeMillis() else aula.dataHoraFim
                val updated = aula.copy(etapa = 5, progressoEtapa = 0, dataHoraFim = newEnd)
                repository.updateAula(updated)

                selectLessonForFlow(details.copy(etapa = 5, progressoEtapa = 0, dataHoraFim = newEnd))
            }
        }
    }

    fun addTimeToLesson(aulaId: Long, extraMinutes: Int) {
        viewModelScope.launch {
            val aula = repository.getAulaById(aulaId)
            if (aula != null) {
                val newDuration = (aula.duracaoMinutos + extraMinutes).coerceAtMost(120)
                repository.updateAula(aula.copy(duracaoMinutos = newDuration))
                
                alerted10Min.remove(aulaId)
                alerted5Min.remove(aulaId)
                alerted1Min.remove(aulaId)
                alertedAulas.remove(aulaId)
                
                val sharedPrefs = getApplication<Application>().getSharedPreferences("valida_moto_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit()
                    .remove("alert_${aulaId}_10min")
                    .remove("alert_${aulaId}_5min")
                    .remove("alert_${aulaId}_1min")
                    .remove("alert_${aulaId}_concluido")
                    .remove("alert_${aulaId}_finalizada")
                    .commit()
                
                if (_warningAlert.value?.aulaId == aulaId) {
                    _warningAlert.value = null
                }
            }
        }
    }

    fun dismissWarningAlert() {
        _warningAlert.value = null
    }

    fun dismissTimeUpAlert() {
        _timeUpAlertLesson.value = null
    }

    fun finalizeFullSession() {
        viewModelScope.launch {
            val activeList = repository.getAulasWithDetailsFlow().first().filter { it.dataHoraFim == 0L && it.statusAula == "pendente" }
            if (activeList.isNotEmpty()) {
                return@launch
            }
            
            if (prefs.isGoogleBackupEnabled && googleDriveService.isInitialized()) {
                googleDriveService.deleteSessionRecovery()
            }
            sessionRecoveryManager.deleteSessionState()
            
            prefs.clearActiveClassState()
            clearActiveStateFlows()
            
            alerted10Min.clear()
            alerted5Min.clear()
            alerted1Min.clear()
            alertedAulas.clear()
            _warningAlert.value = null
            
            auditLog("sessao_encerrada", "Sessão finalizada com sucesso pelo instrutor.")
        }
    }

    fun triggerSilentBackgroundSync() {
        if (!prefs.isGoogleBackupEnabled || !googleDriveService.isInitialized()) return
        viewModelScope.launch {
            try {
                val baseDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return@launch
                val file = File(baseDir, "ValidaMoto_Silent_Backup.zip")
                val success = withContext(Dispatchers.IO) {
                    // Export full silent zip
                    exportFullSilentBackup(file)
                }
                if (success) {
                    val backupResult = withContext(Dispatchers.IO) {
                        googleDriveService.uploadBackup(file, "Silent Background Sync")
                    }
                    Log.d("AulaViewModel", "Silent Background sync status: $backupResult")
                }
            } catch (e: Exception) {
                Log.e("AulaViewModel", "Failed silent sync background", e)
            }
        }
    }

    private suspend fun exportFullSilentBackup(zipFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val context = getApplication<Application>()
            val dbFile = context.getDatabasePath("valida_moto_database")
            val dbShm = context.getDatabasePath("valida_moto_database-shm")
            val dbWal = context.getDatabasePath("valida_moto_database-wal")
            
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
                if (dbFile.exists()) backupHelper.addFileToZip(zipOut, dbFile, "database/valida_moto_database")
                if (dbShm.exists()) backupHelper.addFileToZip(zipOut, dbShm, "database/valida_moto_database-shm")
                if (dbWal.exists()) backupHelper.addFileToZip(zipOut, dbWal, "database/valida_moto_database-wal")
                
                val sharedPrefsFile = File(context.filesDir.parentFile, "shared_prefs/valida_moto_prefs.xml")
                if (sharedPrefsFile.exists()) {
                    backupHelper.addFileToZip(zipOut, sharedPrefsFile, "shared_prefs/valida_moto_prefs.xml")
                }
            }
            true
        } catch (e: Exception) {
            Log.e("AulaViewModel", "Failed to construct full silent backup zip", e)
            false
        }
    }

    fun saveAndUploadSessionState() {
        val sessionId = prefs.activeSessionId
        if (sessionId.isNullOrEmpty()) return
        
        viewModelScope.launch {
            _isCloudSyncing.value = true
            try {
                val firedAlerts = mutableSetOf<Pair<Long, String>>()
                alerted10Min.forEach { firedAlerts.add(it to "10min") }
                alerted5Min.forEach { firedAlerts.add(it to "5min") }
                alerted1Min.forEach { firedAlerts.add(it to "1min") }
                alertedAulas.forEach { firedAlerts.add(it to "finalizada") }
                
                val stateJson = sessionRecoveryManager.buildSessionJson(
                    sessionId = sessionId,
                    instrutorId = prefs.instructorId,
                    instrutorNome = currentInstrutor.value?.nome ?: "Instrutor",
                    activeLessons = activeLessons.value,
                    firedAlerts = firedAlerts
                )
                
                val savedLocal = withContext(Dispatchers.IO) {
                    sessionRecoveryManager.saveSessionState(stateJson)
                }
                
                if (savedLocal && prefs.isGoogleBackupEnabled && googleDriveService.isInitialized()) {
                    val localFile = sessionRecoveryManager.getTempFileForUpload()
                    if (localFile.exists()) {
                        val success = googleDriveService.uploadSessionRecovery(localFile)
                        Log.d("AulaViewModel", "uploadSessionRecovery status: $success")
                    }
                }
            } catch (e: Exception) {
                Log.e("AulaViewModel", "Failed to save and upload session state", e)
            } finally {
                _isCloudSyncing.value = false
            }
        }
    }

    fun discardActiveClass() {
        _showRecoveryDialog.value = false
        viewModelScope.launch {
            val activeList = repository.getAulasWithDetailsFlow().first().filter { it.dataHoraFim == 0L && it.statusAula == "pendente" }
            for (aulaWithDetails in activeList) {
                val aula = repository.getAulaById(aulaWithDetails.id)
                if (aula != null) {
                    val updated = aula.copy(
                        dataHoraFim = System.currentTimeMillis(),
                        statusAula = "cancelada"
                    )
                    repository.updateAula(updated)
                    auditLog("aula_cancelada", "Aula ${aula.id} cancelada ao descartar atividade", alunoId = aula.alunoId, alunoNome = aulaWithDetails.alunoNome, motoId = aula.motoId, motoModelo = aulaWithDetails.motoModelo)
                }
            }
            
            if (prefs.isGoogleBackupEnabled && googleDriveService.isInitialized()) {
                googleDriveService.deleteSessionRecovery()
            }
            sessionRecoveryManager.deleteSessionState()

            prefs.clearActiveClassState()
            clearActiveStateFlows()
            auditLog("sessao_encerrada", "Sessão de atividades descartada pelo usuário.")
        }
    }

    fun refreshTimerOnResume() {
        // Kept for compatibility
    }

    private fun checkBeepsAndVibrations() {
        val now = System.currentTimeMillis()
        val activeList = activeLessons.value
        for (aula in activeList) {
            // Keep in-memory sets in perfect synchronization with SharedPreferences
            if (!isAlertTriggered(aula.id, "10min")) alerted10Min.remove(aula.id)
            if (!isAlertTriggered(aula.id, "5min")) alerted5Min.remove(aula.id)
            if (!isAlertTriggered(aula.id, "1min")) alerted1Min.remove(aula.id)
            if (!isAlertTriggered(aula.id, "concluido")) alertedAulas.remove(aula.id)

            val elapsed = (now - aula.dataHoraInicio) / 1000
            val totalSec = aula.duracaoMinutos * 60
            val remaining = (totalSec - elapsed).toInt()
            
            if (remaining <= 600 && remaining > 300) {
                if (aula.id !in alerted10Min && !isAlertTriggered(aula.id, "10min")) {
                    alerted10Min.add(aula.id)
                    markAlertAsTriggered(aula.id, "10min")
                    soundVib.triggerEmergencyAlert()
                    sendSystemNotification(
                        "Faltam 10 minutos - Autoescola",
                        "A aula de ${aula.alunoNome} termina em 10 minutos!",
                        aula.id,
                        "10min"
                    )
                }
            } else if (remaining <= 300 && remaining > 60) {
                if (aula.id !in alerted5Min && !isAlertTriggered(aula.id, "5min")) {
                    alerted5Min.add(aula.id)
                    markAlertAsTriggered(aula.id, "5min")
                    soundVib.triggerEmergencyAlert()
                    sendSystemNotification(
                        "Faltam 5 minutos - Autoescola",
                        "A aula de ${aula.alunoNome} termina em 5 minutos!",
                        aula.id,
                        "5min"
                    )
                }
            } else if (remaining <= 60 && remaining > 0) {
                if (aula.id !in alerted1Min && !isAlertTriggered(aula.id, "1min")) {
                    alerted1Min.add(aula.id)
                    markAlertAsTriggered(aula.id, "1min")
                    soundVib.triggerEmergencyAlert()
                    sendSystemNotification(
                        "Falta 1 minuto - Autoescola",
                        "Atenção! Resta apenas 1 minuto de aula para ${aula.alunoNome}.",
                        aula.id,
                        "1min"
                    )
                }
            } else if (remaining <= 0) {
                if (aula.id !in alertedAulas && !isAlertTriggered(aula.id, "concluido")) {
                    alertedAulas.add(aula.id)
                    markAlertAsTriggered(aula.id, "concluido")
                    _timeUpAlertLesson.value = aula
                    soundVib.triggerEmergencyAlert()
                    sendSystemNotification(
                        "Aula Finalizada - Autoescola",
                        "A aula de ${aula.alunoNome} chegou ao fim!",
                        aula.id,
                        "concluido"
                    )
                }
            }
        }
    }

    private fun isAlertTriggered(aulaId: Long, alertType: String): Boolean {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("valida_moto_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean("alert_${aulaId}_$alertType", false)
    }

    private fun markAlertAsTriggered(aulaId: Long, alertType: String) {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("valida_moto_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("alert_${aulaId}_$alertType", true).commit()
    }

    private fun scheduleAulaAlarms(aula: AulaWithDetails) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        val totalSec = aula.duracaoMinutos * 60
        val startTime = aula.dataHoraInicio
        
        // Define alert checkpoints
        val alerts = listOf(
            Triple("10min", 600, "Faltam 10 minutos - Autoescola" to "A aula de ${aula.alunoNome} termina em 10 minutos!"),
            Triple("5min", 300, "Faltam 5 minutos - Autoescola" to "A aula de ${aula.alunoNome} termina em 5 minutos!"),
            Triple("1min", 60, "Falta 1 minuto - Autoescola" to "Atenção! Resta apenas 1 minuto de aula para ${aula.alunoNome}."),
            Triple("concluido", 0, "Aula Finalizada - Autoescola" to "A aula de ${aula.alunoNome} chegou ao fim!")
        )
        
        for ((alertType, remainingSec, texts) in alerts) {
            // Already triggered locally or globally?
            if (isAlertTriggered(aula.id, alertType)) continue
            
            val triggerTime = startTime + (totalSec - remainingSec) * 1000
            val now = System.currentTimeMillis()
            
            if (triggerTime > now) {
                val soundDuration = when(alertType) {
                    "10min", "5min" -> 300
                    "1min" -> 800
                    else -> 1500
                }
                val vibeDuration = when(alertType) {
                    "10min", "5min", "1min" -> 200
                    else -> 500
                }
                
                val intent = Intent(context, com.example.feature.aula.presentation.receiver.AulaAlarmReceiver::class.java).apply {
                    putExtra("title", texts.first)
                    putExtra("message", texts.second)
                    putExtra("aulaId", aula.id)
                    putExtra("alertType", alertType)
                    putExtra("soundDurationMs", soundDuration)
                    putExtra("vibeDurationMs", vibeDuration)
                }
                
                val requestCode = (aula.id.toInt() + alertType.hashCode())
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else true

                if (canExact) {
                    try {
                        val showIntent = PendingIntent.getActivity(
                            context,
                            requestCode,
                            Intent(context, com.example.MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
                        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                        Log.d("AulaViewModel", "Scheduled exact alarm clock for $alertType at $triggerTime")
                    } catch (e: Exception) {
                        Log.e("AulaViewModel", "setAlarmClock failed, using inexact fallback for $alertType", e)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        } else {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                        }
                    }
                } else {
                    Log.w("AulaViewModel", "Exact alarms not available, using inexact for $alertType")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                }
            }
        }
    }
    
    private fun cancelAulaAlarms(aulaId: Long) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val alertTypes = listOf("10min", "5min", "1min", "concluido")
        for (alertType in alertTypes) {
            val intent = Intent(context, com.example.feature.aula.presentation.receiver.AulaAlarmReceiver::class.java)
            val requestCode = (aulaId.toInt() + alertType.hashCode())
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d("AulaViewModel", "Cancelled alarm for $alertType")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Avisos de Aula"
            val descriptionText = "Notificações de tempo restante para a aula"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendSystemNotification(title: String, message: String, aulaId: Long, alertType: String) {
        createNotificationChannel()
        val context = getApplication<Application>()
        
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val requestCode = aulaId.toInt() + alertType.hashCode()
        val pendingIntent = if (intent != null) {
            android.app.PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }
        
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(requestCode, builder.build())
        } catch (e: Exception) {
            Log.e("AulaViewModel", "Failed to show system notification", e)
        }
    }

    fun auditLog(tipo: String, descricao: String, alunoId: Long? = null, alunoNome: String? = null, motoId: Long? = null, motoModelo: String? = null) {
        viewModelScope.launch {
            try {
                val currentInst = currentInstrutor.value
                val log = EventoLog(
                    timestamp = System.currentTimeMillis(),
                    tipo = tipo,
                    usuario = currentInst?.nome ?: "Sistema",
                    alunoId = alunoId,
                    alunoNome = alunoNome,
                    instrutorId = currentInst?.id,
                    instrutorNome = currentInst?.nome,
                    motoId = motoId,
                    motoModelo = motoModelo,
                    descricao = descricao
                )
                repository.insertEventoLog(log)
                Log.d("AulaViewModel", "AUDIT_LOG: $tipo - $descricao")
            } catch (e: Exception) {
                Log.e("AulaViewModel", "Failed audit log", e)
            }
        }
    }

    fun createPhotoFile(prefix: String): File {
        val context = getApplication<Application>()
        val dateDirStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        val folderName = when {
            prefix.contains("instrutor") -> {
                "Instrutor_Sessao"
            }
            prefix.contains("aluno") || prefix.contains("add_aluno") -> {
                val activeAlId = if (prefix.contains("add_aluno")) {
                    _addStudentSelectedId.value
                } else {
                    _selectedAlunoId.value
                }
                if (activeAlId != -1L) {
                    "Aluno_$activeAlId"
                } else {
                    "Aluno_Geral"
                }
            }
            else -> {
                val activeAlId = _selectedAlunoId.value
                if (activeAlId != -1L) {
                    "Aluno_$activeAlId"
                } else {
                    "Geral"
                }
            }
        }
        
        val storageDir = File(File(context.filesDir, "photos"), "$folderName/$dateDirStr")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val uniqueId = UUID.randomUUID().toString().take(6)
        return File(storageDir, "${prefix}_${timestamp}_${uniqueId}.jpg")
    }

    // Export Data CSV and ZIP
    suspend fun exportAllDataToDownloads(): File? = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return@withContext null
        val exportDir = File(baseDir, "export_${System.currentTimeMillis()}")
        exportDir.mkdirs()

        val csvFile = File(exportDir, "Relatorio_Aulas.csv")
        try {
            FileOutputStream(csvFile).use { fos ->
                fos.write("ID,Aluno,Instrutor,Placa Moto,Modelo Moto,Inicio,Fim,DuracaoMinutos,KM Inicial,KM Final,KM Percorrido,Status\n".toByteArray())
                allAulas.value.forEach { a ->
                    val start = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(a.dataHoraInicio))
                    val end = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(a.dataHoraFim))
                    val row = "${a.id},\"${a.alunoNome}\",\"${a.instrutorNome}\",\"${a.motoPlaca}\",\"${a.motoModelo}\",$start,$end,${a.duracaoMinutos},${a.kmInicial},${a.kmFinal},${a.kmPercorrido},${a.statusAula}\n"
                    fos.write(row.toByteArray())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val generator = PdfReportGenerator(context)
        val pdfFilesList = mutableListOf<File>()
        allAulas.value.forEach { a ->
            val fotos = repository.getFotosForAula(a.id)
            val file = generator.generateReport(a, fotos)
            if (file != null) {
                pdfFilesList.add(file)
            }
        }

        val zipFile = File(baseDir, "Export_ValidaMoto_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.zip")
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
                backupHelper.addFileToZip(zipOut, csvFile, "Relatorio_Aulas.csv")
                pdfFilesList.forEach { f ->
                    backupHelper.addFileToZip(zipOut, f, "Relatorios/${f.name}")
                }
            }
            zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportPeriodDataToJson(period: String): File? = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return@withContext null
        
        try {
            val now = System.currentTimeMillis()
            
            val classesToExport: List<AulaWithDetails> = allAulas.value.filter { a ->
                if (a.dataHoraFim == 0L) return@filter false
                when (period) {
                    "Hoje" -> {
                        val calNow = Calendar.getInstance()
                        val calAula = Calendar.getInstance().apply { timeInMillis = a.dataHoraFim }
                        calNow.get(Calendar.YEAR) == calAula.get(Calendar.YEAR) &&
                        calNow.get(Calendar.DAY_OF_YEAR) == calAula.get(Calendar.DAY_OF_YEAR)
                    }
                    "7 Dias" -> {
                        a.dataHoraFim >= now - 7L * 24 * 60 * 60 * 1000
                    }
                    "30 Dias" -> {
                        a.dataHoraFim >= now - 30L * 24 * 60 * 60 * 1000
                    }
                    else -> true
                }
            }
            
            val allLogs: List<EventoLog> = repository.getAllLogsFlow().first()
            val logsToExport: List<EventoLog> = allLogs.filter { log ->
                when (period) {
                    "Hoje" -> {
                        val calNow = Calendar.getInstance()
                        val calLog = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                        calNow.get(Calendar.YEAR) == calLog.get(Calendar.YEAR) &&
                        calNow.get(Calendar.DAY_OF_YEAR) == calLog.get(Calendar.DAY_OF_YEAR)
                    }
                    "7 Dias" -> {
                        log.timestamp >= now - 7L * 24 * 60 * 60 * 1000
                    }
                    "30 Dias" -> {
                        log.timestamp >= now - 30L * 24 * 60 * 60 * 1000
                    }
                    else -> true
                }
            }
            
            val rootObj = JSONObject()
            rootObj.put("exportTimestamp", now)
            rootObj.put("exportPeriod", period)
            
            val classesArr = JSONArray()
            for (c in classesToExport) {
                val cObj = JSONObject().apply {
                    put("id", c.id)
                    put("alunoId", c.alunoId)
                    put("alunoNome", c.alunoNome)
                    put("instrutorId", c.instrutorId)
                    put("instrutorNome", c.instrutorNome)
                    put("motoId", c.motoId)
                    put("motoModelo", c.motoModelo)
                    put("motoPlaca", c.motoPlaca)
                    put("dataHoraInicio", c.dataHoraInicio)
                    put("dataHoraFim", c.dataHoraFim)
                    put("duracaoMinutos", c.duracaoMinutos)
                    put("kmInicial", c.kmInicial)
                    put("kmFinal", c.kmFinal)
                    put("kmPercorrido", c.kmPercorrido)
                    put("statusAula", c.statusAula)
                    put("fotoPainelInicio", c.fotoPainelInicio)
                    put("fotoPainelFim", c.fotoPainelFim)
                    put("uuid", c.uuid)
                }
                classesArr.put(cObj)
            }
            rootObj.put("aulas", classesArr)
            
            val logsArr = JSONArray()
            for (l in logsToExport) {
                val lObj = JSONObject().apply {
                    put("id", l.id)
                    put("timestamp", l.timestamp)
                    put("tipo", l.tipo)
                    put("usuario", l.usuario)
                    put("alunoId", l.alunoId ?: JSONObject.NULL)
                    put("alunoNome", l.alunoNome ?: JSONObject.NULL)
                    put("instrutorId", l.instrutorId ?: JSONObject.NULL)
                    put("instrutorNome", l.instrutorNome ?: JSONObject.NULL)
                    put("motoId", l.motoId ?: JSONObject.NULL)
                    put("motoModelo", l.motoModelo ?: JSONObject.NULL)
                    put("descricao", l.descricao)
                }
                logsArr.put(lObj)
            }
            rootObj.put("logs", logsArr)
            
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(baseDir, "Export_Periodo_${period.replace(" ", "")}_$sdf.json")
            FileOutputStream(file).use { out ->
                out.write(rootObj.toString(4).toByteArray())
            }
            
            auditLog("backup_realizado", "Exportacao JSON realizada com sucesso para o periodo: $period")
            file
        } catch (e: Exception) {
            Log.e("AulaViewModel", "Failed to export JSON for period $period", e)
            null
        }
    }



    // --- GOOGLE SIGN IN AND DRIVE BACKUPS SYSTEM FOR SETTINGS ---

    fun onGoogleSignInSuccess(account: GoogleSignInAccount) {
        prefs.googleAccountName = account.displayName
        prefs.googleAccountEmail = account.email
        googleDriveService.initializeWithAccount(account)
        Log.d("AulaViewModel", "REAL_CALL: onGoogleSignInSuccess email=${account.email}")
        viewModelScope.launch {
            checkStateRecoveryOnStart()
        }
    }

    fun saveInstructorDetails(nome: String, cnh: String, validade: String, foto: String) {
        viewModelScope.launch {
            val inst = Instrutor(nome = nome, cnh = cnh, validadeCnh = validade, foto = foto)
            val id = repository.saveInstrutor(inst)
            prefs.instructorId = id
        }
    }

    fun updateCustomLogoPath(path: String?) {
        prefs.customLogoPath = path
        _customLogoPath.value = path
    }

    fun generateBackupMetadata(context: Context): org.json.JSONObject {
        return backupHelper.generateBackupMetadata()
    }

    fun isZipFileValid(zipFile: File): Boolean {
        return backupHelper.isZipFileValid(zipFile)
    }

    fun rotateLocalBackups() {
        backupHelper.rotateLocalBackups()
    }

    suspend fun exportDatabaseBackup(targetFile: File? = null): File? {
        return backupHelper.exportDatabaseBackup(targetFile)
    }

    fun performGoogleDriveBackup(onProgress: (String) -> Unit, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (!googleDriveService.isInitialized()) {
                    onProgress("Erro: Google Drive não inicializado.")
                    onCompleted(false)
                    return@launch
                }
                
                onProgress("Iniciando backup na nuvem...")
                delay(300)
                
                onProgress("Compactando e validando banco de dados...")
                val zipFile = exportDatabaseBackup()
                delay(300)
                
                if (zipFile != null && zipFile.exists()) {
                    onProgress("Enviando dados para o Google Drive...")
                    val metaJson = generateBackupMetadata(getApplication())
                    val success = googleDriveService.uploadBackup(zipFile, metaJson.toString())
                    
                    if (success) {
                        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                        updateLastBackupTime(nowStr)
                        onProgress("Backup sincronizado com sucesso!")
                        auditLog("backup_realizado", "Backup completo enviado para o Google Drive")
                        
                        // Delete temporary local ZIP to save storage
                        if (zipFile.exists()) {
                            zipFile.delete()
                        }
                        
                        delay(600)
                        onCompleted(true)
                    } else {
                        onProgress("Falha ao enviar arquivo para o Google Drive.")
                        delay(1000)
                        onCompleted(false)
                    }
                } else {
                    onProgress("Falha ao gerar e validar o arquivo de backup.")
                    delay(1000)
                    onCompleted(false)
                }
            } catch (e: Exception) {
                Log.e("AulaViewModel", "Google Backup failed", e)
                onProgress("Erro na sincronização: ${e.message}")
                delay(1000)
                onCompleted(false)
            }
        }
    }

    data class GoogleDriveBackupInfo(
        val fileId: String,
        val fileName: String,
        val fileSize: Long,
        val dateStr: String,
        val timeStr: String,
        val version: String,
        val numAlunos: Int,
        val numAulas: Int,
        val numFotos: Int,
        val numPdfs: Int
    )

    suspend fun fetchGoogleDriveBackups(): List<GoogleDriveBackupInfo> {
        val driveFiles = googleDriveService.listBackups()
        return driveFiles.map { file ->
            var version = "Desconhecida"
            var numAlunos = 0
            var numAulas = 0
            var numFotos = 0
            var numPdfs = 0
            
            val desc = file.description
            if (!desc.isNullOrEmpty()) {
                try {
                    val json = org.json.JSONObject(desc)
                    version = json.optString("version", "1.0.0")
                    numAlunos = json.optInt("alunos", 0)
                    numAulas = json.optInt("aulas", 0)
                    numFotos = json.optInt("fotos", 0)
                    numPdfs = json.optInt("pdfs", 0)
                } catch (e: Exception) {
                    Log.e("AulaViewModel", "Failed to parse backup metadata JSON description for ${file.name}", e)
                }
            }
            
            // Parse date & hour from name if possible (e.g., backup_validamoto_yyyyMMdd_HHmmss.zip)
            var dateStr = ""
            var timeStr = ""
            try {
                val datePart = file.name.substringAfterLast("_").substringBefore(".zip")
                val prefixPart = file.name.substringBeforeLast("_")
                val fullTimestamp = prefixPart.substringAfterLast("_") + "_" + datePart // should be yyyyMMdd_HHmmss
                val sdfInput = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val date = sdfInput.parse(fullTimestamp)
                if (date != null) {
                    dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                    timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {
                // fallback using createdTime
                val created = file.createdTime
                if (created != null) {
                    val date = Date(created.value)
                    dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                    timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)
                } else {
                    dateStr = "N/A"
                    timeStr = "N/A"
                }
            }
            
            GoogleDriveBackupInfo(
                fileId = file.id,
                fileName = file.name,
                fileSize = file.getSize() ?: 0L,
                dateStr = dateStr,
                timeStr = timeStr,
                version = version,
                numAlunos = numAlunos,
                numAulas = numAulas,
                numFotos = numFotos,
                numPdfs = numPdfs
            )
        }.sortedByDescending { it.fileName }
    }

    fun performGoogleDriveRestoreById(fileId: String, fileName: String, onProgress: (String) -> Unit, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (!googleDriveService.isInitialized()) {
                    onProgress("Erro: Conta Google desconectada.")
                    onCompleted(false)
                    return@launch
                }
                
                onProgress("Criando backup de segurança do estado atual...")
                delay(300)
                val safetyBackup = exportDatabaseBackup()
                if (safetyBackup != null && safetyBackup.exists()) {
                    Log.d("AulaViewModel", "Safety backup created before restore: ${safetyBackup.name}")
                } else {
                    Log.w("AulaViewModel", "Failed to create safety backup. Proceeding with caution...")
                }
                
                onProgress("Baixando backup da nuvem...")
                val localTempFile = File(getApplication<Application>().cacheDir, "temp_cloud_restore.zip")
                if (localTempFile.exists()) localTempFile.delete()
                
                val downloadSuccess = googleDriveService.downloadFile(fileId, localTempFile)
                if (!downloadSuccess || !localTempFile.exists()) {
                    onProgress("Erro ao baixar o backup da nuvem.")
                    delay(1000)
                    onCompleted(false)
                    return@launch
                }
                
                onProgress("Validando integridade do backup baixado...")
                delay(300)
                if (!isZipFileValid(localTempFile)) {
                    onProgress("Erro: Arquivo baixado está corrompido ou inválido.")
                    if (localTempFile.exists()) localTempFile.delete()
                    delay(1500)
                    onCompleted(false)
                    return@launch
                }
                
                onProgress("Restaurando banco de dados e mídias...")
                val success = restoreFromBackupFile(localTempFile)
                delay(400)
                
                if (localTempFile.exists()) localTempFile.delete()
                
                if (success) {
                    onProgress("Restauração concluída com sucesso! Reiniciando...")
                    auditLog("sincronizacao", "Restaurou backup completo do Google Drive: $fileName")
                    delay(1000)
                    
                    // Restart application
                    val context = getApplication<Application>()
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)
                    onCompleted(true)
                    Runtime.getRuntime().exit(0)
                } else {
                    onProgress("Falha ao aplicar os dados do backup.")
                    delay(1000)
                    onCompleted(false)
                }
            } catch (e: Exception) {
                Log.e("AulaViewModel", "Restore by ID failed", e)
                onProgress("Erro na restauração: ${e.message}")
                delay(1000)
                onCompleted(false)
            }
        }
    }

    private suspend fun restoreFromBackupFile(zipFile: File): Boolean {
        return backupHelper.restoreFromBackupFile(zipFile)
    }
}
