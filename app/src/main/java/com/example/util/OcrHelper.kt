package com.example.util

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OcrHelper(private val context: Context) {
    
    suspend fun recognizeKmFromImage(imageUri: Uri): String? = withContext(Dispatchers.Default) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = Tasks.await(recognizer.process(image))
            
            // Extract only numbers from text, filters for plausible odometer ranges (4 to 7 digits)
            val numbers = result.text.split(Regex("\\D+"))
                .filter { it.length in 4..7 }
                .mapNotNull { it.toIntOrNull() }
            
            numbers.maxOrNull()?.toString()
        } catch (e: Exception) {
            null
        }
    }
}
