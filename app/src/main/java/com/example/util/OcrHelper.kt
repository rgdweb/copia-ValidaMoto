package com.example.util

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper para reconhecimento OCR de hodômetro usando ML Kit on-device.
 *
 * Estrategia de filtragem por contexto multi-camada:
 *  1. Iteracao por lines (preserva contexto visual do painel)
 *  2. Blacklist de keywords de indicadores (KM/H, RPM, TRIP, CLOCK, :, TEMP, FUEL, BATT)
 *  3. Prioridade positiva para linhas contendo "ODO" (hodometro total explicito)
 *  4. Pontuacao multi-criterio: contexto positivo > ausencia de negativos > N digitos > valor
 *
 * Offline-first: usa apenas ML Kit on-device, sem internet nem APIs externas.
 */
class OcrHelper(private val context: Context) {

    // Keywords que indicam que a linha NAO e o hodometro total
    private val negativeKeywords = listOf(
        "km/h", "kmh", "rpm", "trip", "clock", "time",
        "temp", "°c", "°f", "fuel", "gas", "batt", "gear"
    )

    // Keyword que indica que a linha E o hodometro total (prioridade positiva)
    private val positiveKeyword = "odo"

    suspend fun recognizeKmFromImage(imageUri: Uri): String? = withContext(Dispatchers.Default) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result = Tasks.await(recognizer.process(image))

            // Candidatos sobreviventes: List<Triple<numero, pontuacao, valorInt>>
            val candidates = mutableListOf<Triple<String, Int, Int>>()

            // Iterar por lines (preserva contexto visual do painel)
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    val lineText = line.text.trim()
                    if (lineText.isEmpty()) continue

                    val lowerLine = lineText.lowercase()

                    // Camada 2: descartar linhas com indicadores negativos
                    val hasNegative = negativeKeywords.any { kw -> lowerLine.contains(kw) }
                    // Descartar linhas com padrao de horario (relatorio ":")
                    val hasColon = lineText.contains(":")

                    if (hasNegative || hasColon) continue

                    // Camada 3: prioridade positiva para "ODO"
                    val hasPositive = lowerLine.contains(positiveKeyword)

                    // Extrair numeros da linha sobrevivente
                    val numbersInLine = Regex("\\d+").findAll(lineText).map { it.value }.toList()

                    for (numStr in numbersInLine) {
                        val numValue = numStr.toIntOrNull() ?: continue
                        val digitCount = numStr.length

                        // Pontuacao multi-criterio:
                        //  + 1000 se tem "ODO" (contexto positivo explicito)
                        //  + digitCount (favorece hodometros 5-6 digitos sem excluir motos 0 km)
                        //  + valor numerico como desempate final (escala menor)
                        val score = (if (hasPositive) 1000 else 0) + digitCount + (numValue / 100000)

                        candidates.add(Triple(numStr, score, numValue))
                    }
                }
            }

            // Se nao sobrou nenhum candidato, retorna null (UI mostra aviso sutil)
            if (candidates.isEmpty()) return@withContext null

            // Selecionar candidato com maior pontuacao
            // Em caso de empate na pontuacao: maior valor numerico vence (compareBy thenByDescending)
            // Em caso de empate no valor: maxWithOrNull retorna o primeiro encontrado (estabilidade)
            val best = candidates.maxWithOrNull(
                compareByDescending<Triple<String, Int, Int>> { it.second }
                    .thenByDescending { it.third }
            )

            best?.first
        } catch (e: Exception) {
            null
        }
    }
}
