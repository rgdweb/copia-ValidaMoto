package com.example.feature.configuracoes.presentation.screens

import android.net.Uri
import android.os.Environment
import android.widget.Toast
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.BorderStroke
import coil.compose.AsyncImage
import com.example.ui.components.ExportedFilesDialog
import com.example.ui.theme.OrangeAutoescola
import com.example.ui.theme.DarkGrey
import com.example.feature.configuracoes.presentation.ConfiguracoesViewModel
import com.example.util.FileHelper
import com.example.feature.cadastros.presentation.screens.CpfVisualTransformation
import com.example.feature.cadastros.presentation.screens.formatCpf
import java.io.File
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(viewModel: ConfiguracoesViewModel) {
    val instrutor by viewModel.currentInstrutor.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showFilesDialog by remember { mutableStateOf(false) }

    // Instructor Edit fields
    var instNome by remember { mutableStateOf("") }
    var instNumRegistro by remember { mutableStateOf("") }
    var instCategoria by remember { mutableStateOf("") }
    var instUf by remember { mutableStateOf("") }
    var instEmissao by remember { mutableStateOf("") }
    var instCpf by remember { mutableStateOf("") }
    var instFoto by remember { mutableStateOf("") }
    var instPdfPath by remember { mutableStateOf(viewModel.prefs.instructorPdfPath) }
    var instPdfName by remember { mutableStateOf(viewModel.prefs.instructorPdfName) }

    // Sync input states once DB loads
    LaunchedEffect(instrutor) {
        instrutor?.let {
            instNome = it.nome
            instFoto = it.foto
        }
        instCpf = viewModel.prefs.instructorCpf.filter { char -> char.isDigit() }
        instNumRegistro = viewModel.prefs.instructorNumRegistro
        instCategoria = viewModel.prefs.instructorCategoria
        instUf = viewModel.prefs.instructorUf
        instEmissao = viewModel.prefs.instructorEmissao.filter { char -> char.isDigit() }
        instPdfPath = viewModel.prefs.instructorPdfPath
        instPdfName = viewModel.prefs.instructorPdfName
    }

    // Settings States
    var defaultDuration by remember { mutableIntStateOf(viewModel.prefs.defaultDuration) }
    var customDurationStr by remember { mutableStateOf("") }
    var isBeepEnabled by remember { mutableStateOf(viewModel.prefs.isBeepEnabled) }
    var isVibrationEnabled by remember { mutableStateOf(viewModel.prefs.isVibrationEnabled) }

    var customDurationsList by remember {
        mutableStateOf(
            viewModel.prefs.customDurations.split(",")
                .mapNotNull { it.toIntOrNull() }
                .filter { it > 0 }
                .toSet()
        )
    }

    var customGoogleAccountsList by remember {
        mutableStateOf(
            viewModel.prefs.customGoogleAccounts.split(";")
                .filter { it.contains("|") }
                .map {
                    val parts = it.split("|")
                    parts[0] to parts[1]
                }
        )
    }

    // Google Cloud Backup States
    var googleAccountName by remember { mutableStateOf(viewModel.prefs.googleAccountName) }
    var googleAccountEmail by remember { mutableStateOf(viewModel.prefs.googleAccountEmail) }
    var googleLastSyncTime by remember { mutableStateOf(viewModel.prefs.googleLastSyncTime) }
    var isGoogleBackupEnabled by remember { mutableStateOf(viewModel.prefs.isGoogleBackupEnabled) }
    var googleBackupFrequency by remember { mutableStateOf(viewModel.prefs.googleBackupFrequency) }
    
    var showGoogleLoginDialog by remember { mutableStateOf(false) }
    var showGoogleRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showGoogleRestoreListDialog by remember { mutableStateOf(false) }
    var cloudBackupsList by remember { mutableStateOf(emptyList<com.example.feature.configuracoes.presentation.ConfiguracoesViewModel.GoogleDriveBackupInfo>()) }
    var isLoadingCloudBackups by remember { mutableStateOf(false) }
    var selectedBackupForRestore by remember { mutableStateOf<com.example.feature.configuracoes.presentation.ConfiguracoesViewModel.GoogleDriveBackupInfo?>(null) }
    var newAccountName by remember { mutableStateOf("") }
    var newAccountEmail by remember { mutableStateOf("") }
    
    var syncProgressMessage by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }

    // Camera picture for instructor
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoFile?.let {
                instFoto = it.absolutePath
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = viewModel.createPhotoFile("instructor_profile")
            if (copyUriToFile(context, uri, file)) {
                instFoto = file.absolutePath
            } else {
                Toast.makeText(context, "Falha ao carregar imagem da galeria", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val pdfDir = File(context.filesDir, "photos")
            if (!pdfDir.exists()) pdfDir.mkdirs()

            if (instPdfPath.isNotEmpty()) {
                val oldFile = File(instPdfPath)
                if (oldFile.exists()) {
                    oldFile.delete()
                }
            }

            val fileName = getFileNameFromUri(context, uri)
            val destFile = File(pdfDir, "credencial_instrutor.pdf")

            if (copyUriToFile(context, uri, destFile)) {
                val path = destFile.absolutePath
                instPdfPath = path
                instPdfName = fileName
                viewModel.prefs.instructorPdfPath = path
                viewModel.prefs.instructorPdfName = fileName
                Toast.makeText(context, "PDF da credencial salvo!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Falha ao copiar o arquivo PDF.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.onGoogleSignInSuccess(account)
                googleAccountName = account.displayName
                googleAccountEmail = account.email
                Toast.makeText(context, "Conectado: ${account.email}", Toast.LENGTH_LONG).show()
            } catch (e: ApiException) {
                Log.e("AjustesScreen", "Google sign-in failed code=${e.statusCode}", e)
                Toast.makeText(context, "Falha no login Google: código ${e.statusCode}", Toast.LENGTH_LONG).show()
            }
        } else {
            if (data != null) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    task.getResult(ApiException::class.java)
                    Toast.makeText(context, "Login cancelado", Toast.LENGTH_SHORT).show()
                } catch (e: ApiException) {
                    Log.e("AjustesScreen", "Google sign-in canceled/failed code=${e.statusCode}", e)
                    when (e.statusCode) {
                        12501 -> Toast.makeText(context, "Login cancelado pelo usuário", Toast.LENGTH_SHORT).show()
                        10 -> Toast.makeText(context, "Erro de Configuração (Código 10: SHA-1 ou ID de Cliente incorretos no Google Console)", Toast.LENGTH_LONG).show()
                        12500 -> Toast.makeText(context, "Falha de Autenticação (Código 12500: Verifique o Google Play Services)", Toast.LENGTH_LONG).show()
                        else -> Toast.makeText(context, "Login cancelado (Código de Erro: ${e.statusCode})", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(context, "Login cancelado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ajustes ", fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                        Text("do App", fontWeight = FontWeight.Light, color = OrangeAutoescola)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1C1B1F)
                ),
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = Color(0xFFE0E0E0),
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card (Instructor Info)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "CREDENCIAL DO INSTRUTOR DE TRÂNSITO",
                        fontWeight = FontWeight.Bold,
                        color = OrangeAutoescola,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (showPhotoSourceDialog) {
                        AlertDialog(
                            onDismissRequest = { showPhotoSourceDialog = false },
                            title = { Text("Selecionar Foto", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            showPhotoSourceDialog = false
                                            val file = viewModel.createPhotoFile("instructor_profile")
                                            tempPhotoFile = file
                                            tempUri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            cameraLauncher.launch(tempUri!!)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola)
                                    ) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Tirar Foto")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            showPhotoSourceDialog = false
                                            galleryLauncher.launch("image/*")
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Escolher da Galeria")
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showPhotoSourceDialog = false }) {
                                    Text("Cancelar", color = Color.Gray)
                                }
                            }
                        )
                    }

                    // Circle Avatar
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEEEEEE))
                            .border(2.dp, OrangeAutoescola, CircleShape)
                            .align(Alignment.CenterHorizontally)
                            .clickable {
                                showPhotoSourceDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (instFoto.isNotEmpty()) {
                            AsyncImage(
                                model = File(instFoto),
                                contentDescription = "Instructor picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take picture",
                                tint = Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = instNome,
                        onValueChange = { instNome = it },
                        label = { Text("Nome Completo") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = instNumRegistro,
                        onValueChange = { instNumRegistro = it },
                        label = { Text("Nº Registro") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = instCategoria,
                        onValueChange = { instCategoria = it },
                        label = { Text("Categoria") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = instUf,
                        onValueChange = { instUf = it },
                        label = { Text("UF") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = instEmissao,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }.take(8)
                            instEmissao = clean
                        },
                        label = { Text("Emissão (DD/MM/AAAA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        placeholder = { Text("Ex: 12/12/2020") },
                        visualTransformation = DateVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = instCpf,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }.take(11)
                            instCpf = clean
                        },
                        label = { Text("CPF") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        placeholder = { Text("Ex: 000.000.000-00") },
                        visualTransformation = CpfVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Documento Digital da Credencial (PDF)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DarkGrey
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val hasPdf = instPdfPath.isNotEmpty() && File(instPdfPath).exists()

                    Text(
                        text = if (hasPdf) "Arquivo: ${instPdfName.ifEmpty { File(instPdfPath).name }}" else "Nenhum documento selecionado",
                        fontSize = 13.sp,
                        color = if (hasPdf) OrangeAutoescola else Color.Gray,
                        fontWeight = if (hasPdf) FontWeight.Medium else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pdfLauncher.launch("application/pdf") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Selecionar PDF", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { openPdfFile(context, instPdfPath) },
                            enabled = hasPdf,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Abrir PDF", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (instPdfPath.isNotEmpty()) {
                                    val file = File(instPdfPath)
                                    if (file.exists()) {
                                        file.delete()
                                    }
                                }
                                instPdfPath = ""
                                instPdfName = ""
                                viewModel.prefs.instructorPdfPath = ""
                                viewModel.prefs.instructorPdfName = ""
                                Toast.makeText(context, "Documento PDF removido.", Toast.LENGTH_SHORT).show()
                            },
                            enabled = hasPdf,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Remover PDF", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (instNome.isNotEmpty()) {
                                viewModel.prefs.instructorCpf = instCpf
                                viewModel.prefs.instructorNumRegistro = instNumRegistro
                                viewModel.prefs.instructorCategoria = instCategoria
                                viewModel.prefs.instructorUf = instUf
                                viewModel.prefs.instructorEmissao = formatDate(instEmissao)

                                viewModel.saveInstructorDetails(
                                    nome = instNome,
                                    cnh = formatCpf(instCpf),
                                    validade = formatDate(instEmissao),
                                    foto = instFoto
                                )
                                Toast.makeText(context, "Perfil salvo com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Nome é obrigatório", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Salvar Perfil", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // CUSTOM LOGO CONFIGURATION CARD
            val customLogoPath by viewModel.customLogoPath.collectAsState()
            val logoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    try {
                        val path = com.example.util.FileHelper.saveUriToInternalStorage(context, uri, "custom_logo_${System.currentTimeMillis()}")
                        viewModel.updateCustomLogoPath(path)
                        Toast.makeText(context, "Logo personalizado atualizado!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Erro ao carregar logo: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "LOGO PERSONALIZADO",
                        fontWeight = FontWeight.Bold,
                        color = OrangeAutoescola,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Adicione o seu próprio logotipo para ser exibido no cabeçalho do aplicativo ao lado de ValidaMoto.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Current Logo preview
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEEEEEE))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (customLogoPath != null) {
                                AsyncImage(
                                    model = File(customLogoPath!!),
                                    contentDescription = "Logo personalizado",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                AsyncImage(
                                    model = com.example.R.drawable.ic_launcher_foreground_image,
                                    contentDescription = "Logo padrão",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Button(
                                onClick = {
                                    logoPickerLauncher.launch("image/*")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = "Selecionar Foto",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Alterar Logo", fontSize = 13.sp)
                            }

                            if (customLogoPath != null) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateCustomLogoPath(null)
                                        Toast.makeText(context, "Logo padrão restaurado!", Toast.LENGTH_SHORT).show()
                                    },
                                    border = BorderStroke(1.dp, Color.Red),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Restaurar Padrão",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Remover", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Duration Configurations
            DurationSelectionSection(prefs = viewModel.prefs)

            // Haptic Preferences
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "BIPS E VIBRAÇÕES",
                        fontWeight = FontWeight.Bold,
                        color = OrangeAutoescola,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sinais Sonoros (Bips)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "Alertas de 10, 5 e 1 min SEMPRE tocarão por segurança.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isBeepEnabled,
                            onCheckedChange = {
                                isBeepEnabled = it
                                viewModel.prefs.isBeepEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = OrangeAutoescola)
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vibrações no Alerta", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Switch(
                            checked = isVibrationEnabled,
                            onCheckedChange = {
                                isVibrationEnabled = it
                                viewModel.prefs.isVibrationEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = OrangeAutoescola)
                        )
                    }
                }
            }

            // Database Backups Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "SEGURANÇA & OFFLINE",
                        fontWeight = FontWeight.Bold,
                        color = OrangeAutoescola,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Exporte todo o seu banco de dados local com as fotos tiradas em um arquivo compactado ZIP para salvaguarda.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val backupFile = viewModel.exportDatabaseBackup()
                                if (backupFile != null) {
                                    FileHelper.shareFile(context, backupFile)
                                } else {
                                    Toast.makeText(context, "Falha ao gerar Backup", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Backup, contentDescription = "Backup")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fazer Backup Completo (.ZIP)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showFilesDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeAutoescola),
                        border = BorderStroke(1.dp, OrangeAutoescola),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Folder")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver Arquivos & Backups", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Google Cloud Backup Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Cloud",
                            tint = OrangeAutoescola,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "BACKUP & SINCRONIZAÇÃO NUVEM",
                            fontWeight = FontWeight.Bold,
                            color = OrangeAutoescola,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (googleAccountEmail == null) {
                        // Signed out layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = "Cloud",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sincronização Desconectada", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Conecte sua conta Google para salvar backups automáticos de forma segura.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val client = viewModel.googleDriveService.getSignInClient()
                                signInLauncher.launch(client.signInIntent)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5), contentColor = Color.DarkGray),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Google")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fazer Login com Google", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Signed in layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(OrangeAutoescola.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = googleAccountName?.first()?.toString()?.uppercase() ?: "G",
                                    color = OrangeAutoescola,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(googleAccountName ?: "Instrutor Conectado", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(googleAccountEmail ?: "", fontSize = 12.sp, color = Color.Gray)
                            }
                            IconButton(onClick = {
                                viewModel.googleDriveService.signOut()
                                viewModel.prefs.googleAccountName = null
                                viewModel.prefs.googleAccountEmail = null
                                googleAccountName = null
                                googleAccountEmail = null
                            }) {
                                Icon(imageVector = Icons.Default.Logout, contentDescription = "Sair", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0xFFF5F5F5))

                        // Backup Automático Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Backup Automático na Nuvem", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Salvar automaticamente backups de alunos e aulas concluídas no Google Drive.", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = isGoogleBackupEnabled,
                                onCheckedChange = {
                                    isGoogleBackupEnabled = it
                                    viewModel.prefs.isGoogleBackupEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = OrangeAutoescola, checkedTrackColor = OrangeAutoescola.copy(alpha = 0.3f))
                            )
                        }

                        if (isGoogleBackupEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Frequência do Backup:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Diário", "Semanal", "Mensal").forEach { freq ->
                                    FilterChip(
                                        selected = googleBackupFrequency == freq,
                                        onClick = {
                                            googleBackupFrequency = freq
                                            viewModel.prefs.googleBackupFrequency = freq
                                        },
                                        label = { Text(freq) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = OrangeAutoescola,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0xFFF5F5F5))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Último Sincronismo:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    text = googleLastSyncTime ?: "Nunca sincronizado",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Restore Button
                                OutlinedButton(
                                    onClick = {
                                        if (viewModel.googleDriveService.isInitialized()) {
                                            isLoadingCloudBackups = true
                                            showGoogleRestoreListDialog = true
                                            coroutineScope.launch {
                                                cloudBackupsList = viewModel.fetchGoogleDriveBackups()
                                                isLoadingCloudBackups = false
                                            }
                                        } else {
                                            Toast.makeText(context, "Por favor, conecte sua conta Google primeiro.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeAutoescola),
                                    border = BorderStroke(1.dp, OrangeAutoescola),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Restore", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Restaurar", fontSize = 11.sp)
                                }

                                // Sync Now Button
                                Button(
                                    onClick = {
                                        isSyncing = true
                                        viewModel.performGoogleDriveBackup(
                                            onProgress = { msg -> syncProgressMessage = msg },
                                            onCompleted = { success ->
                                                isSyncing = false
                                                if (success) {
                                                    googleLastSyncTime = viewModel.prefs.googleLastSyncTime
                                                    Toast.makeText(context, "Sincronização em nuvem realizada com sucesso!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Falha ao sincronizar com a nuvem.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sincronizar", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilesDialog) {
        ExportedFilesDialog(onDismiss = { showFilesDialog = false })
    }

    // Google Login Dialog Selection
    if (showGoogleLoginDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleLoginDialog = false },
            confirmButton = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google",
                        tint = OrangeAutoescola,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Gerenciamento de Contas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Como este app funciona de forma 100% offline, os backups em nuvem utilizam sua conta para salvar relatórios de forma segura. Adicione sua conta Google personalizada abaixo:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    val presetAccounts = emptyList<Pair<String, String>>()
                    
                    // Combine preset accounts and custom accounts
                    val allAccounts = remember(customGoogleAccountsList) {
                        presetAccounts + customGoogleAccountsList
                    }
                    
                    allAccounts.forEach { (email, name) ->
                        val isPreset = presetAccounts.any { it.first == email }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.prefs.googleAccountName = name
                                        viewModel.prefs.googleAccountEmail = email
                                        googleAccountName = name
                                        googleAccountEmail = email
                                        
                                        // Synchronize local instructor profile with connected Google account
                                        instNome = name
                                        viewModel.saveInstructorDetails(
                                            nome = name,
                                            cnh = formatCpf(instCpf),
                                            validade = formatDate(instEmissao),
                                            foto = instFoto
                                        )
                                        
                                        showGoogleLoginDialog = false
                                        Toast.makeText(context, "Conectado como $name e perfil atualizado!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(OrangeAutoescola.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name.first().toString().uppercase(),
                                            color = OrangeAutoescola,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Column {
                                        Text(text = name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(text = email, fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                                
                                if (!isPreset) {
                                    IconButton(
                                        onClick = {
                                            val newList = customGoogleAccountsList.filter { it.first != email }
                                            customGoogleAccountsList = newList
                                            viewModel.prefs.customGoogleAccounts = newList.map { "${it.first}|${it.second}" }.joinToString(";")
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remover conta",
                                            tint = Color.Red.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)).padding(vertical = 4.dp))
                    
                    Text("CONECTAR NOVA CONTA", fontWeight = FontWeight.Bold, color = OrangeAutoescola, fontSize = 12.sp)
                    
                    OutlinedTextField(
                        value = newAccountName,
                        onValueChange = { newAccountName = it },
                        label = { Text("Nome do Instrutor") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    OutlinedTextField(
                        value = newAccountEmail,
                        onValueChange = { newAccountEmail = it },
                        label = { Text("E-mail Google") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    Button(
                        onClick = {
                            if (newAccountName.isNotEmpty() && newAccountEmail.contains("@")) {
                                val email = newAccountEmail.trim()
                                val name = newAccountName.trim()
                                
                                val newList = customGoogleAccountsList.toMutableList()
                                newList.add(email to name)
                                customGoogleAccountsList = newList
                                viewModel.prefs.customGoogleAccounts = newList.map { "${it.first}|${it.second}" }.joinToString(";")
                                
                                viewModel.prefs.googleAccountName = name
                                viewModel.prefs.googleAccountEmail = email
                                googleAccountName = name
                                googleAccountEmail = email
                                
                                // Automatically sync local instructor details
                                instNome = name
                                viewModel.saveInstructorDetails(
                                    nome = name,
                                    cnh = formatCpf(instCpf),
                                    validade = formatDate(instEmissao),
                                    foto = instFoto
                                )
                                
                                newAccountName = ""
                                newAccountEmail = ""
                                showGoogleLoginDialog = false
                                Toast.makeText(context, "Conectado e perfil atualizado para $name", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Insira um nome e e-mail Google válidos", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Adicionar e Conectar", fontSize = 13.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleLoginDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    // Google Restore Confirm Dialog
    if (showGoogleRestoreConfirmDialog && selectedBackupForRestore != null) {
        val backup = selectedBackupForRestore!!
        AlertDialog(
            onDismissRequest = { showGoogleRestoreConfirmDialog = false },
            title = { Text("Confirmar Restauração") },
            text = {
                Text("Esta ação irá substituir todas as informações locais atuais de candidatos, aulas, fotos, PDFs e configurações pela versão selecionada (${backup.dateStr} às ${backup.timeStr}).\n\nEsta operação não pode ser desfeita. Tem certeza que deseja continuar?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGoogleRestoreConfirmDialog = false
                        showGoogleRestoreListDialog = false
                        isRestoring = true
                        
                        viewModel.performGoogleDriveRestoreById(
                            fileId = backup.fileId,
                            fileName = backup.fileName,
                            onProgress = { msg -> syncProgressMessage = msg },
                            onCompleted = { success ->
                                isRestoring = false
                                selectedBackupForRestore = null
                                if (success) {
                                    Toast.makeText(context, "Dados restaurados com sucesso!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Erro ao restaurar backup da nuvem.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola)
                ) {
                    Text("Sim, Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleRestoreConfirmDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    // Google Restore List Dialog
    if (showGoogleRestoreListDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleRestoreListDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Cloud Backups",
                            tint = OrangeAutoescola,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Backups na Nuvem",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(
                        onClick = {
                            isLoadingCloudBackups = true
                            coroutineScope.launch {
                                cloudBackupsList = viewModel.fetchGoogleDriveBackups()
                                isLoadingCloudBackups = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar",
                            tint = OrangeAutoescola
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    Text(
                        text = "Selecione uma das versões salvas no Google Drive para restaurar o estado completo do aplicativo:",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (isLoadingCloudBackups) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = OrangeAutoescola)
                        }
                    } else if (cloudBackupsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Inbox,
                                    contentDescription = "Sem backups",
                                    modifier = Modifier.size(40.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nenhum backup encontrado na nuvem",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cloudBackupsList) { backup ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedBackupForRestore = backup
                                            showGoogleRestoreConfirmDialog = true
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                    border = BorderStroke(1.dp, Color(0xFFEFEFEF))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Archive,
                                                    contentDescription = "Zip",
                                                    tint = OrangeAutoescola,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${backup.dateStr} - ${backup.timeStr}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            
                                            val sizeKb = backup.fileSize / 1024
                                            val sizeStr = if (sizeKb > 1024) String.format("%.2f MB", sizeKb / 1024.0) else "$sizeKb KB"
                                            Text(
                                                text = sizeStr,
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Alunos: ${backup.numAlunos}  |  Aulas: ${backup.numAulas}",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                            Text(
                                                text = "Fotos: ${backup.numFotos}  |  PDFs: ${backup.numPdfs}",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Versão do App: ${backup.version}",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showGoogleRestoreListDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = OrangeAutoescola)
                ) {
                    Text("Fechar", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Sync progress dialog
    if (isSyncing || isRestoring) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(if (isSyncing) "Sincronizando com Google..." else "Restaurando da Nuvem...") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = OrangeAutoescola)
                    Text(
                        text = syncProgressMessage,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                }
            }
        )
    }
}

private fun copyUriToFile(context: android.content.Context, uri: Uri, destFile: File): Boolean {
    return com.example.util.FileHelper.copyUriToFile(context, uri, destFile)
}

private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var name = ""
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = cursor.getString(index)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    if (name.isEmpty()) {
        name = uri.lastPathSegment?.let { File(it).name } ?: "Credencial.pdf"
    }
    return name
}

private fun openPdfFile(context: android.content.Context, filePath: String) {
    val file = File(filePath)
    if (!file.exists()) {
        Toast.makeText(context, "Arquivo PDF não encontrado.", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(intent, "Abrir Credencial PDF")
        context.startActivity(chooser)
    } catch (e: android.content.ActivityNotFoundException) {
        Toast.makeText(context, "Nenhum aplicativo encontrado para visualizar PDF.", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao abrir o PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun formatDate(input: String): String {
    val clean = input.filter { it.isDigit() }.take(8)
    val sb = StringBuilder()
    for (i in clean.indices) {
        if (i == 2 || i == 4) {
            sb.append('/')
        }
        sb.append(clean[i])
    }
    return sb.toString()
}

class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val formatted = StringBuilder()
        for (i in originalText.indices) {
            if (i == 2 || i == 4) {
                formatted.append('/')
            }
            formatted.append(originalText[i])
        }
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val mapped = when {
                    offset <= 2 -> offset
                    offset <= 4 -> offset + 1
                    else -> offset + 2
                }
                return mapped.coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val mapped = when {
                    offset <= 2 -> offset
                    offset <= 5 -> offset - 1
                    else -> offset - 2
                }
                return mapped.coerceIn(0, originalText.length)
            }
        }
        
        return TransformedText(AnnotatedString(formatted.toString()), offsetMapping)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationSelectionSection(
    prefs: com.example.core.preferences.AppPreferences,
    modifier: Modifier = Modifier,
    title: String = "DURAÇÃO PADRÃO DA AULA"
) {
    val context = LocalContext.current
    var defaultDuration by remember { mutableIntStateOf(prefs.defaultDuration) }
    var customDurationsList by remember {
        mutableStateOf(
            prefs.customDurations
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it > 0 }
                .toSet()
        )
    }
    var customDurationStr by remember { mutableStateOf("") }

    LaunchedEffect(prefs.defaultDuration, prefs.customDurations) {
        defaultDuration = prefs.defaultDuration
        customDurationsList = prefs.customDurations
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .toSet()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = OrangeAutoescola,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            val durationOptions = remember(defaultDuration, customDurationsList) {
                val base = mutableSetOf(30, 50, 60)
                base.addAll(customDurationsList)
                if (!base.contains(defaultDuration)) {
                    base.add(defaultDuration)
                }
                base.sorted()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                durationOptions.forEach { mins ->
                    val isDefaultPreset = listOf(30, 50, 60).contains(mins)
                    FilterChip(
                        selected = defaultDuration == mins,
                        onClick = {
                            defaultDuration = mins
                            prefs.defaultDuration = mins
                            customDurationStr = ""
                        },
                        label = { Text("$mins min") },
                        trailingIcon = if (!isDefaultPreset) {
                            {
                                IconButton(
                                    onClick = {
                                        val newList = customDurationsList.toMutableSet()
                                        newList.remove(mins)
                                        customDurationsList = newList
                                        prefs.customDurations = newList.joinToString(",")
                                        if (defaultDuration == mins) {
                                            defaultDuration = 50
                                            prefs.defaultDuration = 50
                                        }
                                    },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remover",
                                        tint = if (defaultDuration == mins) Color.White else Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeAutoescola,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customDurationStr,
                    onValueChange = {
                        customDurationStr = it
                    },
                    label = { Text("Nova Duração (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = {
                        val customVal = customDurationStr.toIntOrNull()
                        if (customVal != null && customVal > 0) {
                            defaultDuration = customVal
                            prefs.defaultDuration = customVal

                            val newList = customDurationsList.toMutableSet()
                            newList.add(customVal)
                            customDurationsList = newList
                            prefs.customDurations = newList.joinToString(",")

                            customDurationStr = ""
                            Toast.makeText(context, "Duração de $customVal min salva e adicionada!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Insira um número de minutos válido", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Salvar")
                }
            }
        }
    }
}
