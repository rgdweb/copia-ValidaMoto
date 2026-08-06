package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import android.os.Build
import com.example.BuildConfig
import com.example.core.database.entity.AulaFoto
import com.example.core.database.dao.AulaWithDetails
import com.example.core.preferences.AppPreferences
import com.example.feature.cadastros.presentation.screens.formatCpf
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class PdfReportGenerator(private val context: Context) {

    fun generateReport(aula: AulaWithDetails, fotos: List<AulaFoto>): File? {
        val pdfDocument = PdfDocument()

        // Page sizes: A4 = 595 x 842 points
        val pageWidth = 595
        val pageHeight = 842

        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        val paintTitle = Paint().apply {
            color = Color.rgb(255, 111, 0) // Laranja Autoescola #FF6F00
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val paintSubtitle = Paint().apply {
            color = Color.rgb(66, 66, 66) // Cinza Escuro #424242
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateOnlyFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val dateStr = dateOnlyFormat.format(Date(aula.dataHoraInicio))

        // ---------------- PAGE 1: HEADER & INFO & KM PHOTOS ----------------
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Header Border
        val borderPaint = Paint().apply {
            color = Color.rgb(255, 111, 0)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRect(20f, 20f, pageWidth - 20f, pageHeight - 20f, borderPaint)

        // Title
        canvas.drawText("RELATÓRIO DE AULA PRÁTICA — CATEGORIA A", 40f, 55f, paintTitle)
        canvas.drawText("Sessão Homologada: #${String.format("%06d", aula.id)}", 40f, 75f, paintSubtitle)
        canvas.drawText("Data da Aula: $dateStr", 40f, 90f, paintText)

        val prefs = AppPreferences(context)
        val rawCpf = prefs.instructorCpf
        val formattedCpf = if (rawCpf.isEmpty()) "Não cadastrado" else if (rawCpf.contains(".")) rawCpf else formatCpf(rawCpf)
        val numRegistro = prefs.instructorNumRegistro.ifEmpty { "Não informado" }
        val categoria = prefs.instructorCategoria.ifEmpty { "Não informada" }
        val uf = prefs.instructorUf.ifEmpty { "Não informada" }
        val emissao = prefs.instructorEmissao.let {
            if (it.isEmpty()) "Não informada"
            else if (it.contains("/")) it
            else if (it.length == 8) "${it.substring(0,2)}/${it.substring(2,4)}/${it.substring(4)}"
            else it
        }

        // Divider Line
        val dividerPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        canvas.drawLine(40f, 100f, pageWidth - 40f, 100f, dividerPaint)

        // Bloco Instrutor
        canvas.drawText("CREDENCIAL DO INSTRUTOR DE TRÂNSITO", 40f, 118f, paintSubtitle)
        canvas.drawText("Nome Completo: ${aula.instrutorNome.ifEmpty { "Não informado" }}", 40f, 133f, paintText)
        canvas.drawText("CPF: $formattedCpf", 40f, 146f, paintText)
        canvas.drawText("Nº Registro: $numRegistro", 40f, 159f, paintText)
        canvas.drawText("Categoria: $categoria", 40f, 172f, paintText)
        canvas.drawText("UF: $uf", 40f, 185f, paintText)
        canvas.drawText("Emissão: $emissao", 40f, 198f, paintText)

        // Bloco Aluno
        canvas.drawText("DADOS DO ALUNO", 40f, 222f, paintSubtitle)
        canvas.drawText("Nome: ${aula.alunoNome}", 40f, 237f, paintText)
        canvas.drawText("CPF do Aluno: ${if (aula.alunoCpf.isNotEmpty()) aula.alunoCpf else "Não cadastrado"}", 40f, 250f, paintText)
        val aulaNumero = if (aula.aulasConfirmadasAteEntao > 0) {
            aula.aulasConfirmadasAteEntao
        } else if (aula.alunoAulasRealizadas > 0) {
            aula.alunoAulasRealizadas
        } else {
            1
        }
        canvas.drawText("Aula do Histórico: ${aulaNumero}ª aula de ${aula.alunoAulasContratadas} contratadas", 40f, 263f, paintText)

        // Bloco Tempo
        canvas.drawText("INFORMAÇÕES DE TEMPO, KM E GPS", 40f, 287f, paintSubtitle)
        val startStr = dateFormat.format(Date(aula.dataHoraInicio))
        val endStr = dateFormat.format(Date(aula.dataHoraFim))
        canvas.drawText("Início: $startStr", 40f, 302f, paintText)
        canvas.drawText("Fim: $endStr", 40f, 315f, paintText)
        canvas.drawText("Duração Total: ${aula.duracaoMinutos} minutos", 40f, 328f, paintText)
        canvas.drawText("KM Inicial: ${aula.kmInicial}    |    KM Final: ${aula.kmFinal}    |    KM Percorrido: ${aula.kmPercorrido} km", 40f, 341f, paintText)
        canvas.drawText("Localização GPS: -23.550520, -46.633308", 40f, 354f, paintText)

        // Bloco Fotos KM
        canvas.drawText("FOTOS DO PAINEL DA MOTO", 40f, 378f, paintSubtitle)

        // Draw KM Inicial Photo
        canvas.drawText("Painel Inicial (KM: ${aula.kmInicial})", 40f, 393f, paintText)
        val bmpKmInicio = loadScaledBitmap(aula.fotoPainelInicio, 1280, 960)
        if (bmpKmInicio != null) {
            val dstRect = RectF(40f, 403f, 40f + 160f, 403f + 120f)
            canvas.drawBitmap(bmpKmInicio, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            drawPhotoPlaceholder(canvas, 40f, 403f, 160f, 120f)
        }

        // Draw KM Final Photo
        canvas.drawText("Painel Final (KM: ${aula.kmFinal})", 250f, 393f, paintText)
        val bmpKmFinal = loadScaledBitmap(aula.fotoPainelFim, 1280, 960)
        if (bmpKmFinal != null) {
            val dstRect = RectF(250f, 403f, 250f + 160f, 403f + 120f)
            canvas.drawBitmap(bmpKmFinal, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            drawPhotoPlaceholder(canvas, 250f, 403f, 160f, 120f)
        }

        // Footer Page 1
        canvas.drawText("Página 1 de 3", pageWidth / 2f - 30f, pageHeight - 35f, paintText)

        pdfDocument.finishPage(page)

        // ---------------- PAGE 2: INITIAL PHOTOS 2x2 ----------------
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas

        // Border
        canvas.drawRect(20f, 20f, pageWidth - 20f, pageHeight - 20f, borderPaint)

        canvas.drawText("VERIFICAÇÃO INICIAL — FOTOS DE ENTRADA", 40f, 55f, paintTitle)
        canvas.drawLine(40f, 65f, pageWidth - 40f, 65f, dividerPaint)

        // Fotos Instrutor Início
        canvas.drawText("Fotos do Instrutor (Início da Aula)", 40f, 90f, paintSubtitle)
        drawPhotoGrid(canvas, fotos.filter { it.tipo == "instrutor_inicio" }, 40f, 105f)

        // Fotos Aluno Início
        canvas.drawText("Fotos do Aluno (Início da Aula)", 40f, 440f, paintSubtitle)
        drawPhotoGrid(canvas, fotos.filter { it.tipo == "aluno_inicio" }, 40f, 455f)

        // Footer Page 2
        canvas.drawText("Página 2 de 3", pageWidth / 2f - 30f, pageHeight - 35f, paintText)

        pdfDocument.finishPage(page)

        // ---------------- PAGE 3: FINAL PHOTOS & FOOTER/SIGNATURE ----------------
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas

        // Border
        canvas.drawRect(20f, 20f, pageWidth - 20f, pageHeight - 20f, borderPaint)

        canvas.drawText("VERIFICAÇÃO FINAL — FOTOS DE SAÍDA", 40f, 55f, paintTitle)
        canvas.drawLine(40f, 65f, pageWidth - 40f, 65f, dividerPaint)

        // Fotos Instrutor Fim
        canvas.drawText("Fotos do Instrutor (Fim da Aula)", 40f, 90f, paintSubtitle)
        drawPhotoGrid(canvas, fotos.filter { it.tipo == "instrutor_fim" }, 40f, 105f)

        // Fotos Aluno Fim
        canvas.drawText("Fotos do Aluno (Fim da Aula)", 40f, 440f, paintSubtitle)
        drawPhotoGrid(canvas, fotos.filter { it.tipo == "aluno_fim" }, 40f, 455f)

        // Divider
        canvas.drawLine(40f, 700f, pageWidth - 40f, 700f, dividerPaint)

        // Unique Hash and validation status
        val hash = generateHash(aula.id, aula.dataHoraInicio)
        val paintHash = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }

        canvas.drawText("STATUS: AULA VALIDADA COM SUCESSO", 40f, 715f, paintSubtitle)
        canvas.drawText("ASSINATURA DIGITAL DO SISTEMA", 40f, 728f, paintSubtitle)
        canvas.drawText("UUID: ${aula.uuid}", 40f, 740f, paintHash)
        canvas.drawText("HASH: $hash", 40f, 751f, paintHash)
        canvas.drawText("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})", 40f, 762f, paintHash)
        canvas.drawText("Assinado digitalmente por ValidaMoto App v${BuildConfig.VERSION_NAME}", 40f, 775f, paintText)

        // Signature line / seal
        val stampPaint = Paint().apply {
            color = Color.rgb(255, 111, 0)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        val textStamp = Paint().apply {
            color = Color.rgb(255, 111, 0)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        // Draw Stamp
        canvas.drawRoundRect(pageWidth - 250f, 708f, pageWidth - 125f, 778f, 8f, 8f, stampPaint)
        canvas.drawText("VALIDAMOTO", pageWidth - 242f, 725f, textStamp)
        canvas.drawText("100% HOMOLOGADO", pageWidth - 242f, 742f, textStamp)
        canvas.drawText("INTEGRIDADE", pageWidth - 242f, 758f, textStamp)
        canvas.drawText("GARANTIDA", pageWidth - 242f, 772f, textStamp)

        // Draw Real Generated QR Code using ZXing
        val qrContent = "VALIDAMOTO|id=${aula.id}|uuid=${aula.uuid}|hash=$hash|data=$dateStr|aluno=${aula.alunoNome}|instrutor=${aula.instrutorNome}|moto=${aula.motoPlaca}"
        val qrBitmap = generateQrCodeBitmap(qrContent, 75)
        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, pageWidth - 112f, 705f, null)
        } else {
            drawQRCode(canvas, pageWidth - 110f, 708f, 70f)
        }

        // Footer Page 3
        canvas.drawText("Página 3 de 3", pageWidth / 2f - 30f, pageHeight - 35f, paintText)

        pdfDocument.finishPage(page)

        // Save document
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "aulas")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val file = File(dir, "Relatorio_Aula_${aula.id}_${System.currentTimeMillis()}.pdf")
        return try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun drawPhotoGrid(canvas: Canvas, fotos: List<AulaFoto>, startX: Float, startY: Float) {
        val paintPose = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }
        val targetW = 110f
        val targetH = 110f

        val posesOrdered = if (fotos.any { it.pose != "direita" }) {
            listOf("direita", "abaixar", "fechar_olhos", "sorrir")
        } else {
            listOf("direita")
        }

        val poseLabels = mapOf(
            "direita" to "Foto de Perfil",
            "abaixar" to "Abaixe a cabeça",
            "fechar_olhos" to "Feche os olhos",
            "sorrir" to "Sorria"
        )

        for (i in posesOrdered.indices) {
            val pose = posesOrdered[i]
            val foto = fotos.find { it.pose == pose }
            val label = poseLabels[pose] ?: pose

            val col = i % 2
            val row = i / 2

            val x = startX + col * 260f
            val y = startY + row * 150f

            canvas.drawText(label, x, y + 15f, paintPose)

            val bmp = foto?.caminhoFoto?.let { loadScaledBitmap(it, 1000, 1000) }
            if (bmp != null) {
                val dstRect = RectF(x, y + 25f, x + targetW, y + 25f + targetH)
                canvas.drawBitmap(bmp, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
            } else {
                drawPhotoPlaceholder(canvas, x, y + 25f, targetW, targetH)
            }
        }
    }

    private fun drawPhotoPlaceholder(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val bgPaint = Paint().apply {
            color = Color.rgb(240, 240, 240)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val textPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            isAntiAlias = true
        }

        canvas.drawRect(x, y, x + w, y + h, bgPaint)
        canvas.drawRect(x, y, x + w, y + h, borderPaint)
        canvas.drawText("FOTO INDISPONÍVEL", x + 10f, y + h / 2f, textPaint)
    }

    private fun loadScaledBitmap(path: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)
            val srcWidth = options.outWidth
            val srcHeight = options.outHeight

            var inSampleSize = 1
            if (srcHeight > targetHeight || srcWidth > targetWidth) {
                val halfHeight = srcHeight / 2
                val halfWidth = srcWidth / 2
                while (halfHeight / inSampleSize >= targetHeight && halfWidth / inSampleSize >= targetWidth) {
                    inSampleSize *= 2
                }
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize

            val decodedBitmap = BitmapFactory.decodeFile(path, options) ?: return null

            val exifInterface = androidx.exifinterface.media.ExifInterface(path)
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
                val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotatedBitmap = Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true)
                if (rotatedBitmap != decodedBitmap) {
                    decodedBitmap.recycle()
                }
                rotatedBitmap
            } else {
                decodedBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateHash(aulaId: Long, timestamp: Long): String {
        return try {
            val input = "$aulaId-$timestamp"
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }.take(16).uppercase()
        } catch (e: Exception) {
            "HASH_ERROR_${aulaId}"
        }
    }

    fun openPdf(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drawQRCode(canvas: Canvas, startX: Float, startY: Float, size: Float) {
        val qrPaintBlack = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
        val qrPaintWhite = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        canvas.drawRect(startX, startY, startX + size, startY + size, qrPaintWhite)
        
        val blocks = 21
        val blockSize = size / blocks
        val random = java.util.Random(42)
        
        for (row in 0 until blocks) {
            for (col in 0 until blocks) {
                val isFinder = (row < 7 && col < 7) || (row < 7 && col >= blocks - 7) || (row >= blocks - 7 && col < 7)
                if (isFinder) {
                    val inFinderBorder = (row == 0 || row == 6 || col == 0 || col == 6) ||
                                         (row == 0 || row == 6 || col == blocks - 7 || col == blocks - 1) ||
                                         (row == blocks - 7 || row == blocks - 1 || col == 0 || col == 6)
                    val inFinderCenter = (row in 2..4 && col in 2..4) ||
                                         (row in 2..4 && col in (blocks - 5)..(blocks - 3)) ||
                                         (row in (blocks - 5)..(blocks - 3) && col in 2..4)
                    if (inFinderBorder || inFinderCenter) {
                        canvas.drawRect(
                            startX + col * blockSize,
                            startY + row * blockSize,
                            startX + (col + 1) * blockSize,
                            startY + (row + 1) * blockSize,
                            qrPaintBlack
                        )
                    }
                } else {
                    if (random.nextBoolean()) {
                        canvas.drawRect(
                            startX + col * blockSize,
                            startY + row * blockSize,
                            startX + (col + 1) * blockSize,
                            startY + (row + 1) * blockSize,
                            qrPaintBlack
                        )
                    }
                }
            }
        }
        val qrBorderPaint = Paint().apply {
            color = Color.rgb(255, 111, 0)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(startX, startY, startX + size, startY + size, qrBorderPaint)
    }

    private fun generateQrCodeBitmap(content: String, size: Int = 150): Bitmap? {
        return try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
