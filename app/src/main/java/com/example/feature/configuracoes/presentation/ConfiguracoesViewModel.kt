package com.example.feature.configuracoes.presentation

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.database.entity.Aluno
import com.example.core.database.entity.EventoLog
import com.example.core.database.entity.Instrutor
import com.example.core.preferences.AppPreferences
import com.example.core.database.dao.AulaWithDetails
import com.example.data.GoogleDriveService
import com.example.feature.configuracoes.data.repository.ConfiguracoesRepositoryImpl
import com.example.feature.configuracoes.domain.repository.ConfiguracoesRepository
import com.example.feature.configuracoes.domain.usecases.GetInstrutorUseCase
import com.example.util.PdfReportGenerator
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ConfiguracoesViewModel(
    application: Application,
    private val getInstrutorUseCase: GetInstrutorUseCase,
    private val repository: ConfiguracoesRepository
) : AndroidViewModel(application) {

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getDatabase(application)
            val repo = ConfiguracoesRepositoryImpl(db)
            return ConfiguracoesViewModel(
                application = application,
                getInstrutorUseCase = GetInstrutorUseCase(repo),
                repository = repo
            ) as T
        }
    }

    val prefs = AppPreferences(application)
    val googleDriveService = GoogleDriveService(application)

    val currentInstrutor: StateFlow<Instrutor?> = getInstrutorUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _customLogoPath = MutableStateFlow(prefs.customLogoPath)
    val customLogoPath: StateFlow<String?> = _customLogoPath.asStateFlow()

    val allAlunos: StateFlow<List<Aluno>> = repository.getAlunosFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allAulas: StateFlow<List<AulaWithDetails>> = repository.getAulasWithDetailsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun updateCustomLogoPath(path: String?) {
        prefs.customLogoPath = path
        _customLogoPath.value = path
    }

    fun onGoogleSignInSuccess(account: GoogleSignInAccount) {
        prefs.googleAccountName = account.displayName
        prefs.googleAccountEmail = account.email
        googleDriveService.initializeWithAccount(account)
        Log.d("ConfiguracoesViewModel", "REAL_CALL: onGoogleSignInSuccess email=${account.email}")
    }

    fun saveInstructorDetails(nome: String, cnh: String, validade: String, foto: String) {
        viewModelScope.launch {
            val instrutor = Instrutor(
                nome = nome,
                cnh = cnh,
                validadeCnh = validade,
                foto = foto
            )
            val id = repository.saveInstrutor(instrutor)
            prefs.instructorId = id
            auditLog("cadastro_instrutor", "Detalhes do instrutor atualizados")
        }
    }

    private fun auditLog(tipo: String, descricao: String) {
        viewModelScope.launch {
            try {
                val inst = currentInstrutor.value
                repository.insertEventoLog(
                    EventoLog(
                        timestamp = System.currentTimeMillis(),
                        tipo = tipo,
                        usuario = inst?.nome ?: "Sistema",
                        alunoId = null,
                        alunoNome = null,
                        instrutorId = inst?.id,
                        instrutorNome = inst?.nome,
                        motoId = null,
                        motoModelo = null,
                        descricao = descricao
                    )
                )
            } catch (e: Exception) {
                Log.e("ConfiguracoesViewModel", "Failed to insert audit log", e)
            }
        }
    }

    fun createPhotoFile(prefix: String): File {
        val storageDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val uniqueId = UUID.randomUUID().toString().substring(0, 4)
        return File(storageDir, "${prefix}_${timestamp}_${uniqueId}.jpg")
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
        return withContext(Dispatchers.IO) {
            if (!googleDriveService.isInitialized()) return@withContext emptyList()
            try {
                val driveFiles = googleDriveService.listBackups()
                driveFiles.map { file ->
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
                            Log.e("ConfiguracoesViewModel", "Failed to parse backup metadata JSON description for ${file.name}", e)
                        }
                    }
                    
                    var dateStr = ""
                    var timeStr = ""
                    try {
                        val datePart = file.name.substringAfterLast("_").substringBefore(".zip")
                        val prefixPart = file.name.substringBeforeLast("_")
                        val fullTimestamp = prefixPart.substringAfterLast("_") + "_" + datePart
                        val sdfInput = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        val date = sdfInput.parse(fullTimestamp)
                        if (date != null) {
                            dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                            timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date)
                        }
                    } catch (e: Exception) {
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
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
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
                        prefs.googleLastSyncTime = nowStr
                        onProgress("Backup sincronizado com sucesso!")
                        auditLog("backup_realizado", "Backup manual enviado para o Google Drive")
                        
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
                Log.e("ConfiguracoesViewModel", "Google Backup failed", e)
                onProgress("Erro na sincronização: ${e.message}")
                delay(1000)
                onCompleted(false)
            }
        }
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
                    Log.d("ConfiguracoesViewModel", "Safety backup created before restore: ${safetyBackup.name}")
                } else {
                    Log.w("ConfiguracoesViewModel", "Failed to create safety backup. Proceeding with caution...")
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
                Log.e("ConfiguracoesViewModel", "Restore by ID failed", e)
                onProgress("Erro na restauração: ${e.message}")
                delay(1000)
                onCompleted(false)
            }
        }
    }

    suspend fun exportDatabaseBackup(targetFile: File? = null): File? = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val dbFile = context.getDatabasePath("valida_moto_database")
        val dbShm = context.getDatabasePath("valida_moto_database-shm")
        val dbWal = context.getDatabasePath("valida_moto_database-wal")

        val photosDir = File(context.filesDir, "photos")
        val aulasDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "aulas")
        val sharedPrefsFile = File(context.filesDir.parentFile, "shared_prefs/valida_moto_prefs.xml")

        val metadataFile = File(context.cacheDir, "metadata.json")
        try {
            val metaJson = generateBackupMetadata(context)
            metadataFile.writeText(metaJson.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val zipFile = targetFile ?: File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "Backup_ValidaMoto_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.zip"
        )

        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
                if (dbFile.exists()) addFileToZip(zipOut, dbFile, "database/valida_moto_database")
                if (dbShm.exists()) addFileToZip(zipOut, dbShm, "database/valida_moto_database-shm")
                if (dbWal.exists()) addFileToZip(zipOut, dbWal, "database/valida_moto_database-wal")

                if (photosDir.exists()) {
                    addFolderToZip(zipOut, photosDir, "photos")
                }

                if (aulasDir.exists()) {
                    addFolderToZip(zipOut, aulasDir, "aulas")
                }

                if (sharedPrefsFile.exists()) {
                    addFileToZip(zipOut, sharedPrefsFile, "shared_prefs/valida_moto_prefs.xml")
                }

                if (metadataFile.exists()) {
                    addFileToZip(zipOut, metadataFile, "metadata.json")
                }
            }
            
            if (metadataFile.exists()) {
                metadataFile.delete()
            }
            
            if (!isZipFileValid(zipFile)) {
                Log.e("ConfiguracoesViewModel", "Generated ZIP integrity check failed.")
                if (zipFile.exists()) zipFile.delete()
                null
            } else {
                if (targetFile == null) {
                    rotateLocalBackups()
                }
                zipFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun addFolderToZip(zipOut: ZipOutputStream, folder: File, parentPath: String) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            val entryPath = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
            if (file.isDirectory) {
                addFolderToZip(zipOut, file, entryPath)
            } else {
                addFileToZip(zipOut, file, entryPath)
            }
        }
    }

    private fun addFileToZip(zipOut: ZipOutputStream, file: File, zipPath: String) {
        val buffer = ByteArray(4096)
        try {
            BufferedInputStream(FileInputStream(file)).use { bis ->
                val entry = ZipEntry(zipPath)
                zipOut.putNextEntry(entry)
                var count: Int
                while (bis.read(buffer).also { count = it } != -1) {
                    zipOut.write(buffer, 0, count)
                }
                zipOut.closeEntry()
            }
        } catch (e: Exception) {
            Log.e("ConfiguracoesViewModel", "Failed adding file $zipPath to zip", e)
        }
    }

    fun generateBackupMetadata(context: Context): org.json.JSONObject {
        val meta = org.json.JSONObject()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        meta.put("version", getAppVersion(context))
        meta.put("date", sdf.format(Date()))
        meta.put("aulas", allAulas.value.size)
        meta.put("alunos", allAlunos.value.size)
        meta.put("fotos", countFilesRecursively(File(context.filesDir, "photos")))
        val aulasDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "aulas")
        meta.put("pdfs", countFilesRecursively(aulasDir))
        return meta
    }

    fun isZipFileValid(zipFile: File): Boolean {
        if (!zipFile.exists() || zipFile.length() == 0L) return false
        var zipInputStream: java.util.zip.ZipInputStream? = null
        try {
            zipInputStream = java.util.zip.ZipInputStream(BufferedInputStream(FileInputStream(zipFile)))
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                val buffer = ByteArray(4096)
                while (zipInputStream.read(buffer) != -1) {
                    // Read file fully to check integrity
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            return true
        } catch (e: Exception) {
            Log.e("ConfiguracoesViewModel", "ZIP integrity check failed for ${zipFile.name}", e)
            return false
        } finally {
            try {
                zipInputStream?.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun rotateLocalBackups() {
        val context = getApplication<Application>()
        val docDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return
        val files = docDir.listFiles()?.filter { 
            it.isFile && it.name.startsWith("Backup_ValidaMoto_") && it.name.endsWith(".zip")
        }?.sortedByDescending { it.lastModified() } ?: return
        
        if (files.size > 5) {
            val toDelete = files.subList(5, files.size)
            toDelete.forEach { file ->
                Log.d("ConfiguracoesViewModel", "Deleting old local backup: ${file.name}")
                file.delete()
            }
        }
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun countFilesRecursively(dir: File): Int {
        if (!dir.exists()) return 0
        if (dir.isFile) return 1
        var count = 0
        val files = dir.listFiles() ?: return 0
        for (file in files) {
            if (file.isDirectory) {
                count += countFilesRecursively(file)
            } else if (file.isFile) {
                count++
            }
        }
        return count
    }

    private suspend fun restoreFromBackupFile(zipFile: File): Boolean = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val dbFile = context.getDatabasePath("valida_moto_database")
        val dbShm = context.getDatabasePath("valida_moto_database-shm")
        val dbWal = context.getDatabasePath("valida_moto_database-wal")
        val photosDir = File(context.filesDir, "photos")
        val aulasDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "aulas")
        val sharedPrefsFile = File(context.filesDir.parentFile, "shared_prefs/valida_moto_prefs.xml")
        
        AppDatabase.closeDatabase()
        
        try {
            java.util.zip.ZipInputStream(java.io.BufferedInputStream(java.io.FileInputStream(zipFile))).use { zipIn ->
                var entry = zipIn.nextEntry
                val buffer = ByteArray(4096)
                
                while (entry != null) {
                    val name = entry.name
                    if (name.startsWith("database/")) {
                        val targetFile = when {
                            name.endsWith("-shm") -> dbShm
                            name.endsWith("-wal") -> dbWal
                            else -> dbFile
                        }
                        targetFile.parentFile?.mkdirs()
                        java.io.BufferedOutputStream(java.io.FileOutputStream(targetFile)).use { out ->
                            var count: Int
                            while (zipIn.read(buffer).also { count = it } != -1) {
                                out.write(buffer, 0, count)
                            }
                        }
                    } else if (name.startsWith("photos/")) {
                        val fName = name.substringAfter("photos/")
                        val targetFile = File(photosDir, fName)
                        targetFile.parentFile?.mkdirs()
                        java.io.BufferedOutputStream(java.io.FileOutputStream(targetFile)).use { out ->
                            var count: Int
                            while (zipIn.read(buffer).also { count = it } != -1) {
                                out.write(buffer, 0, count)
                            }
                        }
                    } else if (name.startsWith("aulas/")) {
                        val fName = name.substringAfter("aulas/")
                        val targetFile = File(aulasDir, fName)
                        targetFile.parentFile?.mkdirs()
                        java.io.BufferedOutputStream(java.io.FileOutputStream(targetFile)).use { out ->
                            var count: Int
                            while (zipIn.read(buffer).also { count = it } != -1) {
                                out.write(buffer, 0, count)
                            }
                        }
                    } else if (name == "shared_prefs/valida_moto_prefs.xml") {
                        sharedPrefsFile.parentFile?.mkdirs()
                        java.io.BufferedOutputStream(java.io.FileOutputStream(sharedPrefsFile)).use { out ->
                            var count: Int
                            while (zipIn.read(buffer).also { count = it } != -1) {
                                out.write(buffer, 0, count)
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
