package com.example.feature.cadastros.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.preferences.AppPreferences
import com.example.feature.cadastros.data.repository.CadastrosRepositoryImpl
import com.example.feature.cadastros.domain.usecases.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CadastrosViewModel(
    private val getAlunosUseCase: GetAlunosUseCase,
    private val getMotosUseCase: GetMotosUseCase,
    private val getAulasWithDetailsUseCase: GetAulasWithDetailsUseCase,
    private val addStudentUseCase: AddStudentUseCase,
    private val updateStudentUseCase: UpdateStudentUseCase,
    private val deleteStudentUseCase: DeleteStudentUseCase,
    private val addMotoUseCase: AddMotoUseCase,
    private val updateMotoUseCase: UpdateMotoUseCase,
    private val deleteMotoUseCase: DeleteMotoUseCase,
    private val appContext: Context? = null
) : ViewModel() {

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getDatabase(context)
            val repo = CadastrosRepositoryImpl(db)
            return CadastrosViewModel(
                getAlunosUseCase = GetAlunosUseCase(repo),
                getMotosUseCase = GetMotosUseCase(repo),
                getAulasWithDetailsUseCase = GetAulasWithDetailsUseCase(repo),
                addStudentUseCase = AddStudentUseCase(repo),
                updateStudentUseCase = UpdateStudentUseCase(repo),
                deleteStudentUseCase = DeleteStudentUseCase(repo),
                addMotoUseCase = AddMotoUseCase(repo),
                updateMotoUseCase = UpdateMotoUseCase(repo),
                deleteMotoUseCase = DeleteMotoUseCase(repo),
                appContext = context.applicationContext
            ) as T
        }
    }

    val uiState: StateFlow<CadastrosUiState> = combine(
        getAlunosUseCase(),
        getMotosUseCase(),
        getAulasWithDetailsUseCase()
    ) { alunos, motos, aulas ->
        CadastrosUiState(
            alunos = alunos,
            motos = motos,
            aulas = aulas,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CadastrosUiState(isLoading = true)
    )

    fun onEvent(event: CadastrosUiEvent) {
        viewModelScope.launch {
            when (event) {
                is CadastrosUiEvent.AddStudent -> {
                    addStudentUseCase(
                        nome = event.nome,
                        cpf = event.cpf,
                        telefone = event.telefone,
                        contratadas = event.contratadas,
                        realizadas = event.realizadas,
                        status = event.status,
                        exame = event.exame,
                        obs = event.obs,
                        foto = event.foto
                    )
                }
                is CadastrosUiEvent.UpdateStudent -> {
                    updateStudentUseCase(event.aluno)
                }
                is CadastrosUiEvent.DeleteStudent -> {
                    val errorMsg = deleteStudentUseCase(event.aluno, appContext)
                    if (errorMsg != null) {
                        _toastMessage.value = errorMsg
                    } else {
                        _toastMessage.value = "Aluno excluído com sucesso."
                    }
                }
                is CadastrosUiEvent.AddMoto -> {
                    addMotoUseCase(
                        marca = event.marca,
                        modelo = event.modelo,
                        ano = event.ano,
                        placa = event.placa,
                        km = event.km,
                        status = event.status,
                        foto = event.foto
                    )
                }
                is CadastrosUiEvent.UpdateMoto -> {
                    updateMotoUseCase(event.moto)
                }
                is CadastrosUiEvent.DeleteMoto -> {
                    deleteMotoUseCase(event.moto)
                }
            }
        }
    }

    fun createPhotoFile(context: Context, prefix: String): File {
        val prefs = AppPreferences(context)
        val dateDirStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        
        val folderName = when {
            prefix.contains("instrutor") -> {
                "Instrutor_Sessao"
            }
            prefix.contains("aluno") || prefix.contains("add_aluno") -> {
                val activeAlId = if (prefix.contains("add_aluno")) {
                    prefs.addStudentSelectedId
                } else {
                    prefs.activeAlunoId
                }
                if (activeAlId != -1L) {
                    "Aluno_$activeAlId"
                } else {
                    "Aluno_Geral"
                }
            }
            else -> {
                val activeAlId = prefs.activeAlunoId
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
}
