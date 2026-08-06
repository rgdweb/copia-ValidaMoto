package com.example.license

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class LicenseManager(private val context: Context) {

    companion object {
        private const val TAG = "LicenseManager"
        private const val API_BASE = "http://147.15.77.137:8080"
        private const val TOKEN_FILE = "license_token.dat"
        private const val REVALIDATE_AFTER_HOURS = 720L
        private const val GRACE_PERIOD_HOURS = 168L
    }

    enum class LicenseStatus { ACTIVE, NO_LICENSE, EXPIRED, BLOCKED, SERVER_ERROR, APP_OUTDATED, FORCE_UPDATE, UPDATE_AVAILABLE }

    data class LicenseInfo(
        val status: LicenseStatus,
        val clienteNome: String? = null,
        val plano: String? = null,
        val dataExpiracao: String? = null,
        val diasRestantes: Int = 0,
        val errorMessage: String? = null,
        val downloadUrl: String? = null,
        val latestVersionName: String? = null,
        val latestVersionCode: Int = 0,
        val releaseNotes: String? = null
    )

    // Informacao de update disponivel (nao bloqueante) - preenchida em checkLicense()
    @Volatile
    private var pendingUpdateInfo: LicenseInfo? = null
    fun getPendingUpdate(): LicenseInfo? = pendingUpdateInfo
    fun clearPendingUpdate() { pendingUpdateInfo = null }

    // Lembrar qual versao o usuario dispensou (para nao encher ate vir versao mais nova)
    fun dismissUpdate(versionCode: Int) {
        prefs.edit().putInt("last_dismissed_version_code", versionCode).apply()
    }
    fun isUpdateDismissed(versionCode: Int): Boolean {
        return prefs.getInt("last_dismissed_version_code", 0) == versionCode
    }

    private val prefs = context.getSharedPreferences("validamoto_license", Context.MODE_PRIVATE)

    fun getDeviceUuid(): String {
        prefs.getString("device_uuid", null)?.let { return it }
        val bytes = ByteArray(16); SecureRandom().nextBytes(bytes)
        val uuid = bytes.joinToString("") { "%02x".format(it) }
        prefs.edit().putString("device_uuid", uuid).apply()
        return uuid
    }

    fun getDeviceFingerprint(): String {
        val parts = listOf(Build.MANUFACTURER, Build.MODEL,
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        ).joinToString("|")
        return sha256(parts)
    }

    private fun getDeviceInfo(): JSONObject {
        return JSONObject().apply {
            put("modelo", "${Build.MANUFACTURER} ${Build.MODEL}")
            put("manufacturer", Build.MANUFACTURER)
            put("android", Build.VERSION.RELEASE)
        }
    }

    fun getAppVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    private fun checkForceUpdate(response: JSONObject): LicenseInfo? {
        val appVersionObj = response.optJSONObject("app_version") ?: return null
        val minRequiredCode = appVersionObj.optInt("min_required_code", 0)
        val downloadUrl = appVersionObj.optString("download_url", "")
        val latestVersionName = appVersionObj.optString("latest_version_name", null)
        val latestVersionCode = appVersionObj.optInt("latest_version_code", 0)
        val releaseNotes = appVersionObj.optString("release_notes", "")

        val currentVersionCode = getAppVersionCode()

        // Caso 1: FORCE UPDATE - servidor diz que e obrigatorio (min_required_code > current)
        if (minRequiredCode > 0 && currentVersionCode < minRequiredCode) {
            return LicenseInfo(
                status = LicenseStatus.FORCE_UPDATE,
                errorMessage = "Uma nova versão do aplicativo está disponível. Você precisa atualizar para continuar usando.",
                downloadUrl = downloadUrl.ifEmpty { null },
                latestVersionName = latestVersionName,
                latestVersionCode = latestVersionCode,
                releaseNotes = releaseNotes.ifEmpty { null }
            )
        }

        // Caso 2: UPDATE DISPONIVEL (nao obrigatorio) - existe versao mais nova e o usuario ainda nao dispensou
        if (latestVersionCode > currentVersionCode && !isUpdateDismissed(latestVersionCode)) {
            return LicenseInfo(
                status = LicenseStatus.UPDATE_AVAILABLE,
                errorMessage = null,
                downloadUrl = downloadUrl.ifEmpty { null },
                latestVersionName = latestVersionName,
                latestVersionCode = latestVersionCode,
                releaseNotes = releaseNotes.ifEmpty { null }
            )
        }
        return null
    }

    fun checkLicense(): LicenseInfo {
        val token = loadTokenFromDisk()
        if (token == null) return LicenseInfo(status = LicenseStatus.NO_LICENSE, errorMessage = "Ative sua licença para continuar")
        val payload = decodeJwtPayload(token)
        if (payload == null) { clearTokenFromDisk(); return LicenseInfo(status = LicenseStatus.NO_LICENSE, errorMessage = "Licença inválida") }
        val issuedAt = payload.optLong("iat", 0)
        val now = System.currentTimeMillis()
        val hoursSinceIssue = (now - issuedAt * 1000) / (60 * 60 * 1000)
        
        // Token dentro do período de validade (30 dias)
        if (hoursSinceIssue < REVALIDATE_AFTER_HOURS) {
            // CORREÇÃO: Tentar validação online rápida no boot
            // Garante que desativar/bloquear no painel surta efeito imediatamente
            try {
                val apiResponse = validateOnline(token)
                val newToken = apiResponse.optString("token")
                saveTokenToDisk(newToken)
                
                checkForceUpdate(apiResponse)?.let { info ->
                    if (info.status == LicenseStatus.FORCE_UPDATE) return info
                    if (info.status == LicenseStatus.UPDATE_AVAILABLE) pendingUpdateInfo = info
                }

                return parseLicenseFromPayload(decodeJwtPayload(newToken)!!)
            } catch (e: Exception) {
                val unwrapped = if (e is java.util.concurrent.ExecutionException) e.cause ?: e else e
                if (unwrapped is RevokedLicenseException) {
                    clearTokenFromDisk()
                    return LicenseInfo(status = LicenseStatus.BLOCKED, errorMessage = unwrapped.message ?: "Licença revogada ou bloqueada")
                }
                // Sem internet ou servidor indisponível — continuar com token local
                return parseLicenseFromPayload(payload)
            }
        }
        
        // Token entre 30 e 37 dias — tentar revalidar online
        if (hoursSinceIssue < REVALIDATE_AFTER_HOURS + GRACE_PERIOD_HOURS) {
            return try {
                val apiResponse = validateOnline(token)
                val newToken = apiResponse.optString("token")
                saveTokenToDisk(newToken)

                checkForceUpdate(apiResponse)?.let { info ->
                    if (info.status == LicenseStatus.FORCE_UPDATE) return info
                    if (info.status == LicenseStatus.UPDATE_AVAILABLE) pendingUpdateInfo = info
                }

                parseLicenseFromPayload(decodeJwtPayload(newToken)!!)
            } catch (e: Exception) {
                val unwrapped = if (e is java.util.concurrent.ExecutionException) e.cause ?: e else e
                if (unwrapped is RevokedLicenseException) {
                    clearTokenFromDisk()
                    LicenseInfo(status = LicenseStatus.BLOCKED, errorMessage = unwrapped.message ?: "Licença revogada ou bloqueada")
                } else {
                    parseLicenseFromPayload(payload)
                }
            }
        }
        
        return LicenseInfo(status = LicenseStatus.EXPIRED, errorMessage = "Conecte-se à internet para continuar")
    }

    suspend fun activate(licenseKey: String): Result<LicenseInfo> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JSONObject().apply {
                put("license_key", licenseKey); put("device_uuid", getDeviceUuid())
                put("device_fingerprint", getDeviceFingerprint()); put("device_info", getDeviceInfo())
                put("client_time", System.currentTimeMillis() / 1000)
            }
            val response = makeApiCall("/activate.php", requestBody)
            if (response.optBoolean("success")) {
                saveTokenToDisk(response.optString("token"))

                checkForceUpdate(response)?.let { info ->
                    if (info.status == LicenseStatus.FORCE_UPDATE) return@withContext Result.success(info)
                    if (info.status == LicenseStatus.UPDATE_AVAILABLE) pendingUpdateInfo = info
                }

                val license = response.optJSONObject("license")
                Result.success(LicenseInfo(
                    status = LicenseStatus.ACTIVE,
                    clienteNome = license?.optString("cliente_nome"),
                    plano = license?.optString("plano"),
                    dataExpiracao = license?.optString("data_expiracao"),
                    diasRestantes = license?.optInt("dias_restantes", 0) ?: 0
                ))
            } else { Result.failure(Exception(response.optString("message", "Erro ao ativar"))) }
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun validateOnline(token: String): JSONObject {
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        val future = executor.submit<JSONObject> {
            try {
                val requestBody = JSONObject().apply {
                    put("token", token); put("device_uuid", getDeviceUuid())
                    put("client_time", System.currentTimeMillis() / 1000)
                }
                val response = makeApiCall("/validate.php", requestBody)
                if (response.optBoolean("success")) {
                    response
                } else {
                    throw RevokedLicenseException(response.optString("message", "Licença inválida ou revogada"))
                }
            } finally { executor.shutdown() }
        }
        return future.get(20, java.util.concurrent.TimeUnit.SECONDS)
    }

    private fun makeApiCall(path: String, body: JSONObject): JSONObject {
        val url = URL("$API_BASE$path")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 15000; readTimeout = 15000
            setRequestProperty("Content-Type", "application/json"); doOutput = true
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val responseCode = conn.responseCode
        val inputStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
        val response = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
        if (responseCode == 0 || response.isEmpty()) throw Exception("No response from server")
        return JSONObject(response)
    }

    private fun getSecretKey(): SecretKey {
        val keyBytes = getDeviceUuid().toByteArray(Charsets.UTF_8).copyOf(32)
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun saveTokenToDisk(token: String): Boolean {
        return try {
            val plaintext = token.toByteArray(Charsets.UTF_8)
            val iv = ByteArray(12); SecureRandom().nextBytes(iv)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), GCMParameterSpec(128, iv))
            val file = File(context.filesDir, TOKEN_FILE)
            file.outputStream().use { it.write(iv); it.write(cipher.doFinal(plaintext)) }
            true
        } catch (e: Exception) { false }
    }

    private fun loadTokenFromDisk(): String? {
        return try {
            val file = File(context.filesDir, TOKEN_FILE)
            if (!file.exists()) return null
            val fileBytes = file.readBytes()
            if (fileBytes.size <= 12) return null
            val iv = fileBytes.copyOfRange(0, 12); val ciphertext = fileBytes.copyOfRange(12, fileBytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) { null }
    }

    fun clearTokenFromDisk() { try { File(context.filesDir, TOKEN_FILE).delete() } catch (e: Exception) {} }

    private fun decodeJwtPayload(token: String): JSONObject? {
        return try {
            val parts = token.split("."); if (parts.size != 3) return null
            JSONObject(String(Base64.decode(parts[1], Base64.URL_SAFE), Charsets.UTF_8))
        } catch (e: Exception) { null }
    }

    private fun parseLicenseFromPayload(payload: JSONObject): LicenseInfo {
        return LicenseInfo(
            status = LicenseStatus.ACTIVE,
            clienteNome = payload.optString("cliente"), plano = payload.optString("plano"),
            dataExpiracao = payload.optString("expira_em"), diasRestantes = payload.optInt("dias_restantes", 0)
        )
    }

    private fun sha256(input: String): String {
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

class RevokedLicenseException(message: String) : Exception(message)

