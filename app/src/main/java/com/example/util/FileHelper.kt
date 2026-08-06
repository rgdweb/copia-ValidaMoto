package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileHelper {

    fun getExportedFiles(context: Context): List<File> {
        val list = mutableListOf<File>()
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return emptyList()

        // List files in documents root (CSV / ZIP backups / ZIP exports)
        baseDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.name.endsWith(".zip") || file.name.endsWith(".pdf") || file.name.endsWith(".csv"))) {
                list.add(file)
            }
        }

        // List files in "aulas" directory (PDF reports)
        val aulasDir = File(baseDir, "aulas")
        if (aulasDir.exists()) {
            aulasDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".pdf")) {
                    list.add(file)
                }
            }
        }

        // Sort by last modified date (newest first)
        list.sortByDescending { it.lastModified() }
        return list
    }

    fun shareFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mimeType = when {
                file.name.endsWith(".pdf") -> "application/pdf"
                file.name.endsWith(".zip") -> "application/zip"
                file.name.endsWith(".csv") -> "text/csv"
                else -> "*/*"
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Compartilhar com")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao compartilhar arquivo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mimeType = when {
                file.name.endsWith(".pdf") -> "application/pdf"
                file.name.endsWith(".zip") -> "application/zip"
                file.name.endsWith(".csv") -> "text/csv"
                else -> "*/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Nenhum aplicativo encontrado para abrir este tipo de arquivo.", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.US, "%.1f %s", sizeInBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun formatFileDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun saveUriToInternalStorage(context: Context, uri: Uri, fileName: String): String {
        val inputStream = context.contentResolver.openInputStream(uri) ?: throw java.io.FileNotFoundException("Não foi possível ler o arquivo")
        val outputFile = File(context.filesDir, "$fileName.png")
        outputFile.outputStream().use { output ->
            inputStream.use { input ->
                input.copyTo(output)
            }
        }
        return outputFile.absolutePath
    }

    fun copyUriToFile(context: Context, uri: Uri, destFile: File): Boolean {
        return try {
            var rotationDegrees = 0
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                try {
                    val exifInterface = androidx.exifinterface.media.ExifInterface(inputStream)
                    val orientation = exifInterface.getAttributeInt(
                        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                    )
                    rotationDegrees = when (orientation) {
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                } catch (e: Exception) {
                    rotationDegrees = 0
                }
            }

            if (rotationDegrees != 0) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        val rotatedBitmap = android.graphics.Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                        )
                        destFile.outputStream().use { outputStream ->
                            rotatedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, outputStream)
                        }
                        if (rotatedBitmap != bitmap) {
                            rotatedBitmap.recycle()
                        }
                        bitmap.recycle()
                        return true
                    }
                }
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                destFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            normalizeFileOrientation(destFile)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun normalizeFileOrientation(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        return try {
            val exifInterface = androidx.exifinterface.media.ExifInterface(file.absolutePath)
            val orientation = exifInterface.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )
            val rotationDegrees = when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            if (rotationDegrees != 0) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    val rotatedBitmap = android.graphics.Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )
                    file.outputStream().use { outputStream ->
                        rotatedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, outputStream)
                    }
                    if (rotatedBitmap != bitmap) {
                        rotatedBitmap.recycle()
                    }
                    bitmap.recycle()

                    // Reset EXIF orientation tag to ORIENTATION_NORMAL so viewers don't apply rotation twice
                    val newExif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
                    newExif.setAttribute(
                        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL.toString()
                    )
                    newExif.saveAttributes()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
