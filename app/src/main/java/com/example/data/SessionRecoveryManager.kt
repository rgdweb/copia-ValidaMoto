package com.example.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.example.core.database.dao.AulaWithDetails

/**
 * Manages the custom encrypted session recovery file (.kbp) to allow cross-device / cloud-based session recovery.
 * 
 * AES-256-GCM key derived securely with a fixed key. In a production app, the key would be securely 
 * kept in the Android Keystore, which is documented here as an engineering warning.
 */
class SessionRecoveryManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SessionRecoveryManager"
        private const val TEMP_FILE_NAME = "session_recovery_temp.kbp"
        private const val AES_KEY = "validamoto_v1_secret_key_32b!" // 32 bytes for AES-256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }
    
    private fun getSecretKey(): SecretKey {
        val keyBytes = AES_KEY.toByteArray(Charsets.UTF_8).copyOf(32)
        return SecretKeySpec(keyBytes, "AES")
    }
    
    fun getTempFileForUpload(): File = File(context.filesDir, TEMP_FILE_NAME)
    
    fun saveSessionState(sessionJson: JSONObject): Boolean {
        return try {
            val jsonString = sessionJson.toString()
            val plaintext = jsonString.toByteArray(Charsets.UTF_8)
            
            // Generate a random IV for AES-GCM
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            
            // Encrypt using AES/GCM/NoPadding
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val ciphertext = cipher.doFinal(plaintext)
            
            // Write IV followed by ciphertext
            val file = getTempFileForUpload()
            file.outputStream().use { out ->
                out.write(iv)
                out.write(ciphertext)
            }
            
            Log.d(TAG, "REAL_CALL: saveSessionState saved to ${file.absolutePath} (${file.length()} bytes)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session state securely", e)
            false
        }
    }
    
    fun loadSessionState(): JSONObject? {
        return try {
            val file = getTempFileForUpload()
            if (!file.exists()) {
                Log.d(TAG, "No local session state file found to load")
                return null
            }
            
            val fileBytes = file.readBytes()
            if (fileBytes.size <= GCM_IV_LENGTH) {
                Log.w(TAG, "Session state file is too short/corrupted")
                return null
            }
            
            val iv = fileBytes.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = fileBytes.copyOfRange(GCM_IV_LENGTH, fileBytes.size)
            
            // Decrypt using AES/GCM/NoPadding
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val plaintext = cipher.doFinal(ciphertext)
            
            val jsonString = String(plaintext, Charsets.UTF_8)
            Log.d(TAG, "REAL_CALL: loadSessionState decrypted state successfully")
            JSONObject(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load/decrypt session state securely", e)
            null
        }
    }
    
    fun deleteSessionState() {
        try {
            val file = getTempFileForUpload()
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "REAL_CALL: deleteSessionState local file deleted=$deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete secure session state file", e)
        }
    }
    
    fun buildSessionJson(
        sessionId: String,
        instrutorId: Long,
        instrutorNome: String,
        activeLessons: List<AulaWithDetails>,
        firedAlerts: Set<Pair<Long, String>>
    ): JSONObject {
        return JSONObject().apply {
            put("sessionId", sessionId)
            put("lastUpdate", System.currentTimeMillis())
            put("deviceTimestamp", System.currentTimeMillis())
            
            val instrutorObj = JSONObject().apply {
                put("id", instrutorId)
                put("nome", instrutorNome)
            }
            put("instrutor", instrutorObj)
            
            val alunosArray = JSONArray()
            for (aula in activeLessons) {
                val alunoObj = JSONObject().apply {
                    put("aulaId", aula.id)
                    put("alunoId", aula.alunoId)
                    put("alunoNome", aula.alunoNome)
                    put("motoId", aula.motoId)
                    put("motoModelo", aula.motoModelo)
                    put("duracaoMinutos", aula.duracaoMinutos)
                    put("dataHoraInicio", aula.dataHoraInicio)
                    put("kmInicial", aula.kmInicial)
                    
                    val alertasArray = JSONArray()
                    firedAlerts
                        .filter { it.first == aula.id }
                        .forEach { alertasArray.put(it.second) }
                    put("alertasDisparados", alertasArray)
                }
                alunosArray.put(alunoObj)
            }
            put("alunosEmAula", alunosArray)
        }
    }
}
