package com.example.feature.confirmadas.presentation

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.database.dao.AulaWithDetails
import com.example.core.database.entity.EventoLog
import com.example.core.preferences.AppPreferences
import com.example.data.GoogleDriveService
import com.example.feature.confirmadas.data.repository.ConfirmadasRepositoryImpl
import com.example.feature.confirmadas.domain.repository.ConfirmadasRepository
import com.example.feature.confirmadas.domain.usecases.GetAulasWithDetailsUseCase
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
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

class ConfirmadasViewModel(
    application: Application,
    private val getAulasWithDetailsUseCase: GetAulasWithDetailsUseCase,
    private val repository: ConfirmadasRepository
) : AndroidViewModel(application) {

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getDatabase(application)
            val repo = ConfirmadasRepositoryImpl(db)
            return ConfirmadasViewModel(
                application = application,
                getAulasWithDetailsUseCase = GetAulasWithDetailsUseCase(repo),
                repository = repo
            ) as T
        }
    }

    val prefs = AppPreferences(application)
    val googleDriveService = GoogleDriveService(application)

    val allAulas: StateFlow<List<AulaWithDetails>> = getAulasWithDetailsUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun openPdfForHistoricalSession(aulaId: Long) {
        viewModelScope.launch {
            val details = repository.getAulaWithDetailsById(aulaId) ?: return@launch
            val fotos = repository.getFotosForAula(aulaId)
            val generator = PdfReportGenerator(getApplication())
            val file = generator.generateReport(details, fotos)
            if (file != null) {
                generator.openPdf(file)
            }
        }
    }

    fun toggleSyncFlag(aulaId: Long) {
        viewModelScope.launch {
            val aula = repository.getAulaById(aulaId)
            if (aula != null) {
                val newStatus = if (aula.statusAula == "confirmada") "pendente" else "confirmada"
                repository.updateAula(aula.copy(statusAula = newStatus))
                if (newStatus == "pendente") {
                    triggerSilentBackgroundSync()
                }
            }
        }
    }

    private fun triggerSilentBackgroundSync() {
        if (!prefs.isGoogleBackupEnabled || !googleDriveService.isInitialized()) return
        viewModelScope.launch {
            try {
                val baseDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return@launch
                val file = File(baseDir, "ValidaMoto_Silent_Backup.zip")
                val success = withContext(Dispatchers.IO) {
                    exportFullSilentBackup(file)
                }
                if (success) {
                    val backupResult = withContext(Dispatchers.IO) {
                        googleDriveService.uploadBackup(file, "Silent Background Sync")
                    }
                    Log.d("ConfirmadasViewModel", "Silent Background sync status: $backupResult")
                }
            } catch (e: Exception) {
                Log.e("ConfirmadasViewModel", "Failed silent sync background", e)
            }
        }
    }

    private suspend fun exportFullSilentBackup(zipFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val context = getApplication<Application>()
            val dbFile = context.getDatabasePath("valida_moto_database")
            val dbShm = context.getDatabasePath("valida_moto_database-shm")
            val dbWal = context.getDatabasePath("valida_moto_database-wal")
            
            val tempDir = File(context.cacheDir, "silent_zip_temp")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            if (dbFile.exists()) dbFile.copyTo(File(tempDir, dbFile.name), overwrite = true)
            if (dbShm.exists()) dbShm.copyTo(File(tempDir, dbShm.name), overwrite = true)
            if (dbWal.exists()) dbWal.copyTo(File(tempDir, dbWal.name), overwrite = true)

            val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val fotosZipFolder = File(tempDir, "Fotos")
            fotosZipFolder.mkdirs()
            
            picturesDir?.listFiles()?.forEach { f ->
                if (f.isFile && f.extension == "jpg") {
                    f.copyTo(File(fotosZipFolder, f.name), overwrite = true)
                }
            }

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
                if (dbFile.exists()) addFileToZip(zipOut, File(tempDir, dbFile.name), dbFile.name)
                if (dbShm.exists()) addFileToZip(zipOut, File(tempDir, dbShm.name), dbShm.name)
                if (dbWal.exists()) addFileToZip(zipOut, File(tempDir, dbWal.name), dbWal.name)
                
                fotosZipFolder.listFiles()?.forEach { f ->
                    addFileToZip(zipOut, f, "Fotos/${f.name}")
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

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
                addFileToZip(zipOut, csvFile, "Relatorio_Aulas.csv")
                pdfFilesList.forEach { f ->
                    addFileToZip(zipOut, f, "Relatorios/${f.name}")
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
            Log.e("ConfirmadasViewModel", "Failed to export JSON for period $period", e)
            null
        }
    }

    private fun auditLog(tipo: String, descricao: String) {
        viewModelScope.launch {
            try {
                repository.insertEventoLog(
                    EventoLog(
                        timestamp = System.currentTimeMillis(),
                        tipo = tipo,
                        usuario = "Sistema",
                        alunoId = null,
                        alunoNome = null,
                        instrutorId = null,
                        instrutorNome = null,
                        motoId = null,
                        motoModelo = null,
                        descricao = descricao
                    )
                )
            } catch (e: Exception) {
                Log.e("ConfirmadasViewModel", "Failed to insert audit log", e)
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
            e.printStackTrace()
        }
    }
}
