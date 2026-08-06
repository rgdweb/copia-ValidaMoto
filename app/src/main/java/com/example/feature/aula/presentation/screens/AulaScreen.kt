package com.example.feature.aula.presentation.screens

import android.Manifest
import android.content.Context
import kotlinx.coroutines.delay
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.example.util.FileHelper
import com.example.util.OcrHelper
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.OrangeAutoescola
import com.example.ui.theme.DarkGrey
import com.example.feature.aula.presentation.viewmodel.AulaViewModel
import com.example.feature.configuracoes.presentation.screens.DurationSelectionSection
import com.example.core.database.dao.AulaWithDetails
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AulaScreen(
    viewModel: AulaViewModel
) {
    val activeStep by viewModel.activeStep.collectAsState()
    val selectedLessonForFlow by viewModel.selectedLessonForFlow.collectAsState()
    val activeStepToShow = if (selectedLessonForFlow != null) selectedLessonForFlow!!.etapa else (if (activeStep != 0) 4 else 0)
    val activeAulaId by viewModel.activeAulaId.collectAsState()
    val showRecoveryDialog by viewModel.showRecoveryDialog.collectAsState()
    val recoveryInstructor by viewModel.recoveryInstructor.collectAsState()
    val recoveryStartTime by viewModel.recoveryStartTime.collectAsState()
    val recoveryActiveCount by viewModel.recoveryActiveCount.collectAsState()
    val customLogoPath by viewModel.customLogoPath.collectAsState()

    val context = LocalContext.current

    // Handle Active Class Recovery Dialog
    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { /* Force action */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Recovery", tint = OrangeAutoescola)
                    Text("Sessão em Andamento Encontrada", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Há registros de uma sessão anterior não finalizada corretamente.", fontSize = 14.sp)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(text = "• Instrutor: $recoveryInstructor", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(text = "• Horário de início: $recoveryStartTime", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(text = "• Quantidade de alunos ativos: $recoveryActiveCount", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(text = "Deseja continuar a contagem ou finalizar todos de uma vez?", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.restoreActiveClass() },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola)
                ) {
                    Text("Continuar Sessão")
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.finalizeSessionFromRecovery() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Finalizar Sessão", color = Color.White)
                    }
                    TextButton(onClick = { viewModel.cancelRecoveryDialog() }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            }
        )
    }

    // Collect active warning alert and syncing state
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()
    val warningAlert by viewModel.warningAlert.collectAsState()

    // 2. Unified in-app pop-up warnings are now disabled per request. Alerts are purely background system notifications.


    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Safe asynchronous loading of the logo using Coil to prevent crashes
                        var isImageError by remember { mutableStateOf(false) }

                        if (!isImageError) {
                            AsyncImage(
                                model = if (customLogoPath != null) File(customLogoPath!!) else com.example.R.drawable.ic_launcher_foreground_image,
                                contentDescription = "Logo ValidaMoto",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.5.dp, Color.White, RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Fit,
                                onError = {
                                    isImageError = true
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, OrangeAutoescola, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = "Logo ValidaMoto",
                                    tint = OrangeAutoescola,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Valida",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = OrangeAutoescola,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Moto",
                                fontWeight = FontWeight.Light,
                                fontSize = 22.sp,
                                color = DarkGrey,
                                letterSpacing = (-0.5).sp
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(OrangeAutoescola.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "OFFLINE MODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeAutoescola,
                            letterSpacing = 0.5.sp
                        )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (activeStepToShow == 0) {
                // Initial State: No Active Class
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsMotorsports,
                        contentDescription = "Moto",
                        tint = OrangeAutoescola,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Validação de Aula Prática",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inicie o fluxo sequencial de 8 etapas para validar a presença, KM e tempo da aula do candidato.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    Button(
                        onClick = { viewModel.startNewSession() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Iniciar Nova Aula", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // State Machine UI (Steps 1 to 8)
                val stepName = when (activeStepToShow) {
                    1 -> "Fotos do Instrutor (Entrada)"
                    2 -> "Fotos do Aluno (Entrada)"
                    3 -> "Leitura de KM (Início)"
                    4 -> "Aula em Curso"
                    5 -> "Fotos do Instrutor (Saída)"
                    6 -> "Fotos do Aluno (Saída)"
                    7 -> "Leitura de KM (Fim)"
                    8 -> "Validação / PDF"
                    else -> ""
                }

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Step Indicator Bar
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF5F5F5)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "STATUS DO FLUXO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkGrey.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Passo $activeStepToShow de 8: $stepName",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C1B1F)
                                )
                            }

                            // Row of pills
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 1..8) {
                                        val isActive = i == activeStepToShow
                                        val isCompleted = i < activeStepToShow
                                        val color = if (isActive || isCompleted) OrangeAutoescola else Color(0xFFE0E0E0)
                                        val width = if (isActive) 20.dp else 10.dp
                                        Box(
                                            modifier = Modifier
                                                .size(width = width, height = 4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(color)
                                        )
                                    }
                                }

                                if (activeStepToShow != 4 && activeStepToShow != 8) {
                                    IconButton(
                                        onClick = { viewModel.returnToSession() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Sair do fluxo",
                                            tint = Color.Red,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Content inside container, child steps handle their own scrolling
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Step UI Router with Slide transition effect
                        AnimatedContent(
                            targetState = activeStepToShow,
                            transitionSpec = {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            }, label = "StepTransition"
                        ) { step ->
                        when (step) {
                            1 -> StepInstructorPhotos(viewModel, "inicio")
                            2 -> StepStudentPhotos(viewModel, "inicio")
                            3 -> StepInitialKm(viewModel)
                            4 -> StepClassInProgress(viewModel)
                            5 -> StepInstructorPhotos(viewModel, "fim")
                            6 -> StepStudentPhotos(viewModel, "fim")
                            7 -> StepFinalKm(viewModel)
                            8 -> StepValidatedSuccess(viewModel)
                        }
                    }
                }
            }
        }
    }
}
}

// Reuseable front camera photo guide
@Composable
fun StepInstructorPhotos(viewModel: AulaViewModel, stage: String) {
    val selectedLessonForFlow by viewModel.selectedLessonForFlow.collectAsState()
    val poseIndex = selectedLessonForFlow?.progressoEtapa ?: 0
    val context = LocalContext.current

    val poses = listOf("direita")
    val poseInstructions = listOf(
        "foto de perfil"
    )

    val currentPose = poses[poseIndex]
    val currentInstruction = poseInstructions[poseIndex]

    // Capture Uri using rememberSaveable to survive process recreation
    var tempPhotoPath by rememberSaveable { mutableStateOf("") }
    var photoTakenPath by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            if (tempPhotoPath.isNotEmpty()) {
                FileHelper.normalizeFileOrientation(File(tempPhotoPath))
                photoTakenPath = tempPhotoPath
            }
        } else {
            Toast.makeText(context, "Falha ao capturar foto", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = viewModel.createPhotoFile("instrutor_${stage}_$currentPose")
            if (copyUriToFile(context, uri, file)) {
                photoTakenPath = file.absolutePath
            } else {
                Toast.makeText(context, "Falha ao carregar imagem da galeria", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permissão de câmera concedida. Clique em 'Tirar Foto' novamente.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "A permissão de câmera é necessária para tirar fotos.", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (stage == "inicio") "Fotos de Entrada — Instrutor" else "Fotos de Saída — Instrutor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OrangeAutoescola
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Foto ${poseIndex + 1} de 1",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Large text instruction (min 20.sp)
            Text(
                text = currentInstruction,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Photo Preview or Placeholder
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, OrangeAutoescola, RoundedCornerShape(16.dp))
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                if (photoTakenPath != null) {
                    AsyncImage(
                        model = File(photoTakenPath!!),
                        contentDescription = "Foto capturada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Face guide",
                            modifier = Modifier.size(60.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Câmera Frontal", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            if (photoTakenPath == null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasCameraPermission) {
                                val file = viewModel.createPhotoFile("instrutor_${stage}_$currentPose")
                                tempPhotoPath = file.absolutePath
                                val authority = "${context.packageName}.fileprovider"
                                val uri = FileProvider.getUriForFile(context, authority, file)
                                cameraLauncher.launch(uri)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tirar Foto", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escolher da Galeria", fontSize = 16.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoTakenPath = null
                            viewModel.repeatPosePhoto()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Refazer", fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.handlePosePhoto("instrutor_$stage", currentPose, photoTakenPath!!)
                            photoTakenPath = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Próxima", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StepStudentPhotos(viewModel: AulaViewModel, stage: String) {
    val selectedLessonForFlow by viewModel.selectedLessonForFlow.collectAsState()
    val poseIndex = selectedLessonForFlow?.progressoEtapa ?: 0
    val allAlunos by viewModel.allAlunos.collectAsState()
    val selectedAlunoId by viewModel.selectedAlunoId.collectAsState()
    val context = LocalContext.current

    val poses = listOf("direita")
    val poseInstructions = listOf(
        "foto de perfil"
    )

    val currentPose = poses[poseIndex]
    val currentInstruction = poseInstructions[poseIndex]

    // Capture Uri using rememberSaveable to survive process recreation
    var tempPhotoPath by rememberSaveable { mutableStateOf("") }
    var photoTakenPath by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            if (tempPhotoPath.isNotEmpty()) {
                photoTakenPath = tempPhotoPath
            }
        } else {
            Toast.makeText(context, "Falha ao capturar foto", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = viewModel.createPhotoFile("aluno_${stage}_$currentPose")
            if (copyUriToFile(context, uri, file)) {
                photoTakenPath = file.absolutePath
            } else {
                Toast.makeText(context, "Falha ao carregar imagem da galeria", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permissão de câmera concedida. Clique em 'Tirar Foto' novamente.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "A permissão de câmera é necessária para tirar fotos.", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (stage == "inicio") "Fotos de Entrada — Aluno" else "Fotos de Saída — Aluno",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OrangeAutoescola
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dropdown Selector (Only required at start of class, at final step it's already selected)
            if (stage == "inicio" && poseIndex == 0) {
                var expanded by remember { mutableStateOf(false) }
                val selectedAluno = allAlunos.find { it.id == selectedAlunoId }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Selecione o Aluno:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .clickable { expanded = true }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedAluno?.nome ?: "Clique para selecionar...",
                                color = if (selectedAluno != null) Color.Black else Color.Gray
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "dropdown")
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        allAlunos.forEach { al ->
                            DropdownMenuItem(
                                text = { Text("${al.nome} - Categoria A (${al.status})") },
                                onClick = {
                                    viewModel.selectStudent(al.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                val selectedAluno = allAlunos.find { it.id == selectedAlunoId }
                Text(
                    text = "Aluno: ${selectedAluno?.nome ?: ""}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Foto ${poseIndex + 1} de 1",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Large text instruction (min 20.sp)
            Text(
                text = currentInstruction,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Photo Preview
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, OrangeAutoescola, RoundedCornerShape(16.dp))
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                if (photoTakenPath != null) {
                    AsyncImage(
                        model = File(photoTakenPath!!),
                        contentDescription = "Foto capturada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Portrait,
                            contentDescription = "Face guide",
                            modifier = Modifier.size(60.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Câmera Frontal", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            val isStudentSelected = selectedAlunoId != -1L

            if (photoTakenPath == null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isStudentSelected) {
                                Toast.makeText(context, "Selecione um aluno primeiro", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasCameraPermission) {
                                val file = viewModel.createPhotoFile("aluno_${stage}_$currentPose")
                                tempPhotoPath = file.absolutePath
                                val authority = "${context.packageName}.fileprovider"
                                val uri = FileProvider.getUriForFile(context, authority, file)
                                cameraLauncher.launch(uri)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isStudentSelected) OrangeAutoescola else Color.Gray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tirar Foto", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (!isStudentSelected) {
                                Toast.makeText(context, "Selecione um aluno primeiro", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isStudentSelected) OrangeAutoescola else Color.Gray)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escolher da Galeria", fontSize = 16.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoTakenPath = null
                            viewModel.repeatPosePhoto()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Refazer", fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.handlePosePhoto("aluno_$stage", currentPose, photoTakenPath!!)
                            photoTakenPath = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Próxima", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StepInitialKm(viewModel: AulaViewModel) {
    val allMotos by viewModel.allMotos.collectAsState()
    val selectedMotoId by viewModel.selectedMotoId.collectAsState()
    val kmInicialInput by viewModel.kmInicialInput.collectAsState()
    val fotoPainelInicio by viewModel.fotoPainelInicio.collectAsState()
    val context = LocalContext.current

    // Estados locais para feedback do OCR (nao persistem, apenas UI)
    var ocrLoading by remember { mutableStateOf(false) }
    var ocrError by remember { mutableStateOf(false) }

    // Take Picture Launcher using rememberSaveable to survive process recreation
    var tempPhotoPath by rememberSaveable { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            if (tempPhotoPath.isNotEmpty()) {
                FileHelper.normalizeFileOrientation(File(tempPhotoPath))
                viewModel.setFotoPainelInicio(tempPhotoPath)
            }
        } else {
            Toast.makeText(context, "Falha ao capturar foto do painel", Toast.LENGTH_SHORT).show()
        }
    }

    // OCR: dispara apenas quando a foto do painel muda (chave exclusiva = fotoPainelInicio)
    // Nao re-dispara quando setKmInicial atualiza o StateFlow (chave nao depende de KM)
    LaunchedEffect(fotoPainelInicio) {
        // Ignora estado inicial vazio (sem foto ainda)
        if (fotoPainelInicio.isNullOrEmpty()) return@LaunchedEffect

        ocrLoading = true
        ocrError = false
        try {
            val ocrHelper = OcrHelper(context)
            val uri = Uri.fromFile(File(fotoPainelInicio!!))
            val result = ocrHelper.recognizeKmFromImage(uri)
            if (result != null) {
                viewModel.setKmInicial(result)
                ocrLoading = false
            } else {
                ocrLoading = false
                ocrError = true
            }
        } catch (e: Exception) {
            ocrLoading = false
            ocrError = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permissão de câmera concedida. Toque novamente para tirar a foto.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "A permissão de câmera é necessária para tirar fotos.", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Medição Inicial",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OrangeAutoescola
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Moto Selection Dropdown
            var expanded by remember { mutableStateOf(false) }
            val selectedMoto = allMotos.find { it.id == selectedMotoId }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Selecione a Moto:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .clickable { expanded = true }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedMoto?.let { "${it.marca} ${it.modelo} [${it.placa}]" } ?: "Clique para selecionar...",
                            color = if (selectedMoto != null) Color.Black else Color.Gray
                        )
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "dropdown")
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    allMotos.forEach { moto ->
                        DropdownMenuItem(
                            text = { Text("${moto.marca} ${moto.modelo} (${moto.placa}) - KM: ${moto.kmAtual}") },
                            onClick = {
                                viewModel.selectMoto(moto.id)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Camera button for panel
            Text(
                text = "Foto do Painel Inicial:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (!fotoPainelInicio.isNullOrEmpty()) {
                    AsyncImage(
                        model = File(fotoPainelInicio),
                        contentDescription = "Foto painel inicial",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasCameraPermission) {
                                val file = viewModel.createPhotoFile("painel_inicio")
                                tempPhotoPath = file.absolutePath
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                cameraLauncher.launch(uri)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "cam", modifier = Modifier.size(48.dp), tint = OrangeAutoescola)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tirar foto do painel (Câmera Traseira)", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // KM manual typing (mandatory)
            Text(
                text = "Quilometragem Inicial (KM):",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = kmInicialInput,
                onValueChange = { viewModel.setKmInicial(it) },
                singleLine = true,
                enabled = !ocrLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(if (ocrLoading) "Lendo painel..." else "Digitação manual obrigatória")
                },
                trailingIcon = {
                    if (ocrLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                supportingText = {
                    when {
                        ocrLoading -> Text("Processando OCR do painel...", color = OrangeAutoescola)
                        ocrError -> Text("Não foi possível ler o KM automaticamente. Digite manualmente.", color = Color.Red)
                        else -> Text("")
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            val isFormComplete = selectedMotoId != -1L && kmInicialInput.isNotEmpty() && !fotoPainelInicio.isNullOrEmpty()

            Button(
                onClick = {
                    if (isFormComplete) {
                        viewModel.confirmAndStartClass()
                    } else {
                        Toast.makeText(context, "Selecione a moto, tire foto e digite o KM", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isFormComplete) OrangeAutoescola else Color.Gray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirmar e Iniciar Aula", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StepClassInProgress(
    viewModel: AulaViewModel
) {
    val isAddingStudent by viewModel.isAddingStudent.collectAsState()
    if (isAddingStudent) {
        AddStudentWizard(viewModel)
    } else {
        ActiveSessionDashboard(viewModel)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentWizard(viewModel: AulaViewModel) {
    val step by viewModel.addStudentStep.collectAsState()
    val allAlunos by viewModel.allAlunos.collectAsState()
    val allMotos by viewModel.allMotos.collectAsState()
    val activeLessons by viewModel.activeLessons.collectAsState()

    val selectedStudentId by viewModel.addStudentSelectedId.collectAsState()
    val selectedMotoId by viewModel.addStudentSelectedMotoId.collectAsState()
    val kmInicial by viewModel.addStudentKmInicial.collectAsState()
    val fotoPainel by viewModel.addStudentFotoPainel.collectAsState()
    val poseIndex by viewModel.addStudentPoseIndex.collectAsState()

    val context = LocalContext.current

    var wizardError by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permissão de câmera concedida! Clique em tirar foto novamente.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissão de câmera é necessária para tirar fotos.", Toast.LENGTH_LONG).show()
        }
    }

    // Move state & activity result launchers to top level of composable unconditionally
    var step2TempPhotoPath by rememberSaveable { mutableStateOf("") }
    var step2PhotoTakenPath by rememberSaveable { mutableStateOf<String?>(null) }

    val step2CameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        try {
            if (success && step2TempPhotoPath.isNotEmpty()) {
                FileHelper.normalizeFileOrientation(File(step2TempPhotoPath))
                step2PhotoTakenPath = step2TempPhotoPath
            }
        } catch (e: Exception) {
            wizardError = "Erro no retorno da câmera (Foto Aluno): ${e.message}\n${e.stackTraceToString()}"
        }
    }

    val step2GalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        try {
            if (uri != null) {
                val prefix = if (step == 2) "add_instrutor" else "add_aluno"
                val file = viewModel.createPhotoFile("${prefix}_direita")
                if (copyUriToFile(context, uri, file)) {
                    step2PhotoTakenPath = file.absolutePath
                } else {
                    wizardError = "Falha ao carregar imagem da galeria"
                }
            }
        } catch (e: Exception) {
            wizardError = "Erro no retorno da galeria: ${e.message}"
        }
    }

    var step3TempPhotoPath by rememberSaveable { mutableStateOf("") }

    val step3CameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        try {
            if (success && step3TempPhotoPath.isNotEmpty()) {
                FileHelper.normalizeFileOrientation(File(step3TempPhotoPath))
                viewModel.setAddStudentFotoPainel(step3TempPhotoPath)
            }
        } catch (e: Exception) {
            wizardError = "Erro no retorno da câmera (Foto Painel): ${e.message}\n${e.stackTraceToString()}"
        }
    }

    // Filter out already active students
    val activeStudentIds = activeLessons.map { it.alunoId }
    val availableAlunos = allAlunos.filter { it.id !in activeStudentIds && it.status != "Concluído" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Wizard Header Row with close/cancel button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NOVO ALUNO NA SESSÃO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeAutoescola,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Etapa $step de 6",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = { viewModel.cancelAddingStudent() }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Cancelar", tint = Color.Red)
            }
        }

        LinearProgressIndicator(
            progress = { step / 6f },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp)),
            color = OrangeAutoescola,
            trackColor = Color(0xFFEEEEEE)
        )

        when (step) {
            1 -> {
                // Select student
                Text("Selecione o Candidato:", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                
                if (availableAlunos.isEmpty()) {
                    Text(
                        text = "Nenhum aluno disponível para iniciar aula hoje. Todos os alunos já estão em aula ou concluíram.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                } else {
                    availableAlunos.forEach { al ->
                        val isSelected = selectedStudentId == al.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectAddStudent(al.id) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, if (isSelected) OrangeAutoescola else Color(0xFFEEEEEE)),
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) OrangeAutoescola.copy(alpha = 0.05f) else Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.selectAddStudent(al.id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = OrangeAutoescola)
                                )
                                Column {
                                    Text(text = al.nome, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(text = "Aulas realizadas: ${al.aulasRealizadas}/${al.aulasContratadas}", fontSize = 13.sp, color = DarkGrey)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.advanceAddStudentStep(2) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedStudentId != -1L
                    ) {
                        Text("Avançar para Fotos", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            2 -> {
                // Pose photos - Instructor
                val poses = listOf("direita")
                val poseInstructions = listOf(
                    "foto de perfil"
                )

                val currentPose = poses[poseIndex]
                val currentInstruction = poseInstructions[poseIndex]

                Text(
                    text = "Foto ${poseIndex + 1} de 1 (Instrutor — Entrada)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = currentInstruction,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = OrangeAutoescola
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, OrangeAutoescola, RoundedCornerShape(16.dp))
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    if (step2PhotoTakenPath != null) {
                        AsyncImage(
                            model = File(step2PhotoTakenPath!!),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Face, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (step2PhotoTakenPath == null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasCameraPermission) {
                                        val file = viewModel.createPhotoFile("add_instrutor_${currentPose}")
                                        step2TempPhotoPath = file.absolutePath
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        step2CameraLauncher.launch(uri)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                } catch (e: Exception) {
                                    wizardError = "Erro ao tirar foto (Foto Instrutor): ${e.message}\n${e.stackTraceToString()}"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tirar Foto", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                step2GalleryLauncher.launch("image/*")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Escolher da Galeria")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { step2PhotoTakenPath = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Refazer", fontSize = 16.sp)
                        }
                        Button(
                            onClick = {
                                viewModel.handleAddStudentPosePhoto(step2PhotoTakenPath!!)
                                step2PhotoTakenPath = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirmar Foto", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            3 -> {
                // Pose photos - Candidate
                val poses = listOf("direita")
                val poseInstructions = listOf(
                    "foto de perfil"
                )

                val currentPose = poses[poseIndex]
                val currentInstruction = poseInstructions[poseIndex]

                Text(
                    text = "Foto ${poseIndex + 1} de 1 (Candidato — Entrada)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = currentInstruction,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = OrangeAutoescola
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, OrangeAutoescola, RoundedCornerShape(16.dp))
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    if (step2PhotoTakenPath != null) {
                        AsyncImage(
                            model = File(step2PhotoTakenPath!!),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Face, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (step2PhotoTakenPath == null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasCameraPermission) {
                                        val file = viewModel.createPhotoFile("add_aluno_${currentPose}")
                                        step2TempPhotoPath = file.absolutePath
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        step2CameraLauncher.launch(uri)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                } catch (e: Exception) {
                                    wizardError = "Erro ao tirar foto (Foto Aluno): ${e.message}\n${e.stackTraceToString()}"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tirar Foto", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                step2GalleryLauncher.launch("image/*")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Escolher da Galeria")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { step2PhotoTakenPath = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Refazer", fontSize = 16.sp)
                        }
                        Button(
                            onClick = {
                                viewModel.handleAddStudentPosePhoto(step2PhotoTakenPath!!)
                                step2PhotoTakenPath = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirmar Foto", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            4 -> {
                // Vehicle selection
                Text("Vincular Veículo e Painel:", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                var expanded by remember { mutableStateOf(false) }
                val selectedMoto = allMotos.find { it.id == selectedMotoId }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Selecione a Moto:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .clickable { expanded = true }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedMoto?.let { "${it.marca} ${it.modelo} [${it.placa}]" } ?: "Clique para selecionar...",
                                color = if (selectedMoto != null) Color.Black else Color.Gray
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        allMotos.forEach { moto ->
                            DropdownMenuItem(
                                text = { Text("${moto.marca} ${moto.modelo} (${moto.placa}) - KM: ${moto.kmAtual}") },
                                onClick = {
                                    viewModel.selectAddMoto(moto.id, moto.kmAtual)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Panel photo
                Text("Foto do Painel Inicial:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!fotoPainel.isNullOrEmpty()) {
                        AsyncImage(
                            model = File(fotoPainel),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                try {
                                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasCameraPermission) {
                                        val file = viewModel.createPhotoFile("add_painel_inicio")
                                        step3TempPhotoPath = file.absolutePath
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        step3CameraLauncher.launch(uri)
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                } catch (e: Exception) {
                                    wizardError = "Erro ao tirar foto (Foto Painel): ${e.message}\n${e.stackTraceToString()}"
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = OrangeAutoescola, modifier = Modifier.size(36.dp))
                            Text("Tirar foto do painel (Câmera Traseira)", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Quilometragem Inicial (KM):", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = kmInicial,
                    onValueChange = { viewModel.setAddStudentKmInicial(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Digitação obrigatória") }
                )

                Spacer(modifier = Modifier.height(24.dp))

                val isReady = selectedMotoId != -1L && kmInicial.isNotEmpty() && !fotoPainel.isNullOrEmpty()
                Button(
                    onClick = { viewModel.advanceAddStudentStep(5) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isReady) OrangeAutoescola else Color.Gray),
                    shape = RoundedCornerShape(12.dp),
                    enabled = isReady
                ) {
                    Text("Avançar para Duração", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            5 -> {
                // Select Duration
                Text("Selecionar Duração da Aula:", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                DurationSelectionSection(
                    prefs = viewModel.prefs,
                    title = "SELECIONE A DURAÇÃO DA AULA"
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.advanceAddStudentStep(6) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Avançar para Resumo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            6 -> {
                // Confirm/Summary
                Text("Resumo do Aluno e Aula:", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                val selectedStudent = allAlunos.find { it.id == selectedStudentId }
                val selectedMoto = allMotos.find { it.id == selectedMotoId }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "Candidato: ${selectedStudent?.nome}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "Moto: ${selectedMoto?.marca} ${selectedMoto?.modelo} (${selectedMoto?.placa})", fontSize = 14.sp)
                        Text(text = "KM Inicial: $kmInicial", fontSize = 14.sp)
                        Text(text = "Duração de Aula: ${viewModel.prefs.defaultDuration} min", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.confirmAndAddStudent() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Iniciar Aula do Candidato", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    if (wizardError != null) {
        AlertDialog(
            onDismissRequest = { wizardError = null },
            title = { Text("Erro no Fluxo de Fotos", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ocorreu uma falha no fluxo da câmera:", color = Color.Red, fontWeight = FontWeight.Bold)
                    Text(wizardError ?: "", style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp))
                }
            },
            confirmButton = {
                Button(
                    onClick = { wizardError = null },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola)
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun StepFinalKm(viewModel: AulaViewModel) {
    val kmInicialInput by viewModel.kmInicialInput.collectAsState()
    val kmFinalInput by viewModel.kmFinalInput.collectAsState()
    val fotoPainelFim by viewModel.fotoPainelFim.collectAsState()
    val context = LocalContext.current

    // Estados locais para feedback do OCR (nao persistem, apenas UI)
    var ocrLoading by remember { mutableStateOf(false) }
    var ocrError by remember { mutableStateOf(false) }

    // Take Picture Launcher using rememberSaveable to survive process recreation
    var tempPhotoPath by rememberSaveable { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            if (tempPhotoPath.isNotEmpty()) {
                FileHelper.normalizeFileOrientation(File(tempPhotoPath))
                viewModel.setFotoPainelFim(tempPhotoPath)
            }
        } else {
            Toast.makeText(context, "Falha ao capturar foto do painel", Toast.LENGTH_SHORT).show()
        }
    }

    // OCR: dispara apenas quando a foto do painel final muda (chave exclusiva = fotoPainelFim)
    // Nao re-dispara quando setKmFinal atualiza o StateFlow (chave nao depende de KM)
    LaunchedEffect(fotoPainelFim) {
        // Ignora estado inicial vazio (sem foto ainda)
        if (fotoPainelFim.isNullOrEmpty()) return@LaunchedEffect

        ocrLoading = true
        ocrError = false
        try {
            val ocrHelper = OcrHelper(context)
            val uri = Uri.fromFile(File(fotoPainelFim!!))
            val result = ocrHelper.recognizeKmFromImage(uri)
            if (result != null) {
                viewModel.setKmFinal(result)
                ocrLoading = false
            } else {
                ocrLoading = false
                ocrError = true
            }
        } catch (e: Exception) {
            ocrLoading = false
            ocrError = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permissão de câmera concedida. Toque novamente para tirar a foto.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "A permissão de câmera é necessária para tirar fotos.", Toast.LENGTH_LONG).show()
        }
    }

    val kmIni = kmInicialInput.filter { it.isDigit() }.toIntOrNull() ?: 0
    val kmFin = kmFinalInput.filter { it.isDigit() }.toIntOrNull() ?: 0
    val kmPercorrido = kmFin - kmIni

    Card(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Medição Final",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OrangeAutoescola
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Display Initial KM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Quilometragem Inicial:", fontWeight = FontWeight.SemiBold)
                Text("$kmIni KM", fontWeight = FontWeight.Bold, color = OrangeAutoescola)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Camera button for final panel
            Text(
                text = "Foto do Painel Final:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (!fotoPainelFim.isNullOrEmpty()) {
                    AsyncImage(
                        model = File(fotoPainelFim),
                        contentDescription = "Foto painel final",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasCameraPermission) {
                                val file = viewModel.createPhotoFile("painel_final")
                                tempPhotoPath = file.absolutePath
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                cameraLauncher.launch(uri)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "cam", modifier = Modifier.size(48.dp), tint = OrangeAutoescola)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tirar foto do painel final (Câmera Traseira)", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // KM manual typing
            Text(
                text = "Quilometragem Final (KM):",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = kmFinalInput,
                onValueChange = { viewModel.setKmFinal(it) },
                singleLine = true,
                enabled = !ocrLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(if (ocrLoading) "Lendo painel..." else "Digitação manual obrigatória")
                },
                trailingIcon = {
                    if (ocrLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                supportingText = {
                    when {
                        ocrLoading -> Text("Processando OCR do painel...", color = OrangeAutoescola)
                        ocrError -> Text("Não foi possível ler o KM automaticamente. Digite manualmente.", color = Color.Red)
                        else -> Text("")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // KM calculated
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OrangeAutoescola.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quilometragem Percorrida:", fontWeight = FontWeight.Bold)
                Text(
                    text = "${if (kmPercorrido > 0) kmPercorrido else 0} KM",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = OrangeAutoescola
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            val isFormComplete = kmFinalInput.isNotEmpty() && !fotoPainelFim.isNullOrEmpty() && kmFin >= kmIni

            Button(
                onClick = {
                    if (isFormComplete) {
                        viewModel.finalizeFinalKmAndClass()
                    } else if (kmFin < kmIni && kmFinalInput.isNotEmpty()) {
                        Toast.makeText(context, "KM final não pode ser menor que o inicial ($kmIni)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Complete a foto e o KM final", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isFormComplete) OrangeAutoescola else Color.Gray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Finalizar Aula", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StepValidatedSuccess(viewModel: AulaViewModel) {
    val allAulas by viewModel.allAulas.collectAsState()
    val activeAulaId by viewModel.activeAulaId.collectAsState()
    val allAlunos by viewModel.allAlunos.collectAsState()

    val currentAula = allAulas.find { it.id == activeAulaId }
    val student = allAlunos.find { it.id == currentAula?.alunoId }

    Card(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big green check circle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Aula Validada com Sucesso!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Summary Table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryRow("Aluno:", currentAula?.alunoNome ?: "")
                SummaryRow("Instrutor:", currentAula?.instrutorNome ?: "")
                SummaryRow(
                    "Data:",
                    currentAula?.dataHoraInicio?.let {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                    } ?: ""
                )
                SummaryRow(
                    "Duração:",
                    "${currentAula?.duracaoMinutos ?: 50} minutos"
                )
                SummaryRow(
                    "KM Percorrido:",
                    "${currentAula?.kmPercorrido ?: 0} KM"
                )
                SummaryRow(
                    "Contrato Aluno:",
                    "${student?.aulasRealizadas ?: 0} / ${student?.aulasContratadas ?: 20} aulas"
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = { viewModel.generateAndOpenPdfForActiveSession() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver Relatório PDF", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.returnToSession() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkGrey),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voltar para Sessão", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { viewModel.startNewSession() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Iniciar Outra Sessão", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        Text(text = value, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CheckoutStepPhotos(
    title: String,
    instructionPrefix: String,
    poseIndex: Int,
    onPhotoTaken: (String, String) -> Unit,
    createPhotoFile: (String) -> File
) {
    val context = LocalContext.current
    val poses = listOf("direita")
    val poseInstructions = listOf(
        "foto de perfil"
    )
    val currentPose = poses[poseIndex]
    val currentInstruction = poseInstructions[poseIndex]

    var tempPhotoPath by rememberSaveable { mutableStateOf("") }
    var photoTakenPath by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            if (tempPhotoPath.isNotEmpty()) {
                FileHelper.normalizeFileOrientation(File(tempPhotoPath))
                photoTakenPath = tempPhotoPath
            }
        } else {
            Toast.makeText(context, "Falha ao capturar foto", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = createPhotoFile(currentPose)
            if (copyUriToFile(context, uri, file)) {
                photoTakenPath = file.absolutePath
            } else {
                Toast.makeText(context, "Falha ao carregar imagem da galeria", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permissão concedida. Clique em 'Tirar Foto' novamente.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissão de câmera necessária.", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OrangeAutoescola
            )
            Text(
                text = "Foto ${poseIndex + 1} de 1",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = instructionPrefix,
                fontSize = 15.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Text(
                text = currentInstruction,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Preview
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, OrangeAutoescola, RoundedCornerShape(16.dp))
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                if (photoTakenPath != null) {
                    AsyncImage(
                        model = File(photoTakenPath!!),
                        contentDescription = "Foto capturada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Face guide",
                            modifier = Modifier.size(50.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Câmera Frontal", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (photoTakenPath == null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasCameraPermission) {
                                val file = createPhotoFile(currentPose)
                                tempPhotoPath = file.absolutePath
                                val authority = "${context.packageName}.fileprovider"
                                val uri = FileProvider.getUriForFile(context, authority, file)
                                cameraLauncher.launch(uri)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tirar Foto", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escolher da Galeria", fontSize = 16.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { photoTakenPath = null },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                    ) {
                        Text("Refazer", fontSize = 15.sp)
                    }
                    Button(
                        onClick = {
                            onPhotoTaken(photoTakenPath!!, currentPose)
                            photoTakenPath = null
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Próxima", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutStepKmAndPanel(
    lesson: AulaWithDetails,
    kmFinal: String,
    onKmChange: (String) -> Unit,
    fotoPainelPath: String?,
    onFotoPainelTaken: (String?) -> Unit,
    createPhotoFile: () -> File,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    var tempPhotoPath by rememberSaveable { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            if (tempPhotoPath.isNotEmpty()) {
                FileHelper.normalizeFileOrientation(File(tempPhotoPath))
                onFotoPainelTaken(tempPhotoPath)
            }
        } else {
            Toast.makeText(context, "Falha ao capturar foto do painel", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permissão concedida. Clique em 'Tirar Foto' novamente.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissão de câmera necessária.", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Medições e Registro Final",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OrangeAutoescola
            )

            // Info Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFFF9C4))
                    .padding(12.dp)
            ) {
                Text(
                    text = "KM Inicial registrado na entrada: ${lesson.kmInicial} km",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF57F17)
                )
            }

            // KM Input
            OutlinedTextField(
                value = kmFinal,
                onValueChange = { onKmChange(it) },
                label = { Text("Quilometragem Final (KM)") },
                placeholder = { Text("Ex: ${lesson.kmInicial + 5}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeAutoescola,
                    focusedLabelColor = OrangeAutoescola
                )
            )

            // Foto Painel title
            Text(
                text = "Foto do Painel da Moto (Fim)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            // Dashboard Preview or Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, OrangeAutoescola.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                if (fotoPainelPath != null) {
                    AsyncImage(
                        model = File(fotoPainelPath),
                        contentDescription = "Foto do painel de saída",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = "Odometer guide",
                            modifier = Modifier.size(50.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tire a foto mostrando o hodômetro final", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            // Camera Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasCameraPermission) {
                            val file = createPhotoFile()
                            tempPhotoPath = file.absolutePath
                            val authority = "${context.packageName}.fileprovider"
                            val uri = FileProvider.getUriForFile(context, authority, file)
                            cameraLauncher.launch(uri)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1.5f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (fotoPainelPath == null) OrangeAutoescola else Color.Gray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (fotoPainelPath == null) "Capturar Painel" else "Refazer Foto", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val kmVal = kmFinal.toIntOrNull() ?: 0
            val isKmValid = kmVal > lesson.kmInicial
            val isReady = isKmValid && fotoPainelPath != null

            Button(
                onClick = { onConfirm() },
                enabled = isReady,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), disabledContainerColor = Color.LightGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Complete")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar e Finalizar Aula", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            if (kmFinal.isNotEmpty() && !isKmValid) {
                Text(
                    text = "A quilometragem final deve ser maior que a quilometragem inicial (${lesson.kmInicial}).",
                    color = Color.Red,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionDashboard(
    viewModel: AulaViewModel
) {
    val activeLessons by viewModel.activeLessons.collectAsState()
    val allAlunos by viewModel.allAlunos.collectAsState()
    val allMotos by viewModel.allMotos.collectAsState()
    val currentInstrutor by viewModel.currentInstrutor.collectAsState()
    val currentTimeMillis by viewModel.currentTimeMillis.collectAsState()
    val allAulas by viewModel.allAulas.collectAsState()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTimerOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.refreshTimerOnResume()
    }
    
    val context = LocalContext.current

    // Request notification permission for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Avisos em segundo plano estão desativados sem permissão.", Toast.LENGTH_SHORT).show()
        }
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Observer for timeUpAlertLesson
    val timeUpAlertLesson by viewModel.timeUpAlertLesson.collectAsState()
    
    timeUpAlertLesson?.let { lesson ->
        var showDialogAddTime by remember { mutableStateOf(false) }
        
        if (showDialogAddTime) {
            val maxExtra = 120 - lesson.duracaoMinutos
            AlertDialog(
                onDismissRequest = { showDialogAddTime = false },
                title = { Text("Adicionar Tempo de Aula", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Selecione quantos minutos deseja acrescentar para ${lesson.alunoNome}. O limite máximo por aula é de 2 horas (120 minutos).", fontSize = 14.sp)
                        Text("Duração atual: ${lesson.duracaoMinutos} min. (Pode adicionar até $maxExtra min.)", fontSize = 13.sp, color = Color.Gray)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 30, 45, 60).forEach { mins ->
                                if (mins <= maxExtra) {
                                    Button(
                                        onClick = {
                                            viewModel.addTimeToLesson(lesson.id, mins)
                                            showDialogAddTime = false
                                            viewModel.dismissTimeUpAlert()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    ) {
                                        Text("+$mins min", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showDialogAddTime = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { viewModel.dismissTimeUpAlert() },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta",
                            tint = Color.Red,
                            modifier = Modifier.size(28.dp)
                        )
                        Text("AULA FINALIZADA!", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 18.sp)
                    }
                },
                text = {
                    Text("O tempo de aula de ${lesson.alunoNome} chegou ao fim!", fontSize = 15.sp)
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.startIndividualCheckout(lesson.id)
                                viewModel.dismissTimeUpAlert()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Iniciar Checkout (Tirar Fotos)")
                        }
                        
                        val maxExtra = 120 - lesson.duracaoMinutos
                        if (maxExtra > 0) {
                            OutlinedButton(
                                onClick = { showDialogAddTime = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeAutoescola),
                                border = BorderStroke(1.dp, OrangeAutoescola),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Acrescentar + Tempo")
                            }
                        }
                        
                        TextButton(
                            onClick = { viewModel.dismissTimeUpAlert() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Dispensar", color = Color.Gray)
                        }
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Instructor summary banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SportsMotorsports,
                    contentDescription = null,
                    tint = OrangeAutoescola,
                    modifier = Modifier.size(40.dp)
                )
                Column {
                    Text(
                        text = "INSTRUTOR DA SESSÃO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGrey
                    )
                    Text(
                        text = currentInstrutor?.nome ?: "Ricardo Silva",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Session Statistics Banner
        val todayStart = remember {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val completedToday = remember(allAulas) {
            allAulas.count {
                it.statusAula == "confirmada" && it.dataHoraFim >= todayStart
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ALUNOS ATIVOS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                        Text(
                            text = "${activeLessons.size}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1C1B1F)
                        )
                    }
                }
                
                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(35.dp)
                        .background(Color(0xFFE0E0E0))
                )

                // Completed column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CONCLUÍDOS HOJE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = OrangeAutoescola,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$completedToday",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1C1B1F)
                        )
                    }
                }
            }
        }

        // Discrete Backup Indicator
        val pendingCount = remember(allAulas) {
            allAulas.count { it.statusAula == "pendente" && it.dataHoraFim > 0L }
        }
        val lastSyncTime = viewModel.prefs.googleLastSyncTime ?: "Nunca"
        val isBackupEnabled = viewModel.prefs.isGoogleBackupEnabled && !viewModel.prefs.googleAccountEmail.isNullOrEmpty()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (!isBackupEnabled) Color(0xFFF5F5F5) else if (pendingCount > 0) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
            ),
            border = BorderStroke(1.dp, if (!isBackupEnabled) Color(0xFFE0E0E0) else if (pendingCount > 0) Color(0xFFFFCC80) else Color(0xFFC8E6C9))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (!isBackupEnabled) Icons.Default.Close 
                                      else if (pendingCount > 0) Icons.Default.Warning 
                                      else Icons.Default.CheckCircle,
                        contentDescription = "Status do Backup",
                        tint = if (!isBackupEnabled) Color.Gray 
                               else if (pendingCount > 0) Color(0xFFE65100) 
                               else Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (!isBackupEnabled) "Backup em nuvem desativado"
                               else if (pendingCount > 0) "$pendingCount aula(s) aguardando backup"
                               else "Sincronizado: $lastSyncTime",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (!isBackupEnabled) Color.Gray 
                                else if (pendingCount > 0) Color(0xFFE65100) 
                                else Color(0xFF2E7D32)
                    )
                }
                
                if (isBackupEnabled && pendingCount > 0) {
                    Text(
                        text = "Sincronizando...",
                        fontSize = 10.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color(0xFFE65100).copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Add student action card
        Button(
            onClick = { viewModel.startAddingStudent() },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Adicionar Aluno à Sessão", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Text(
            text = "ALUNOS EM AULA (${activeLessons.size})",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGrey,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (activeLessons.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                border = BorderStroke(1.dp, Color(0xFFEFEFEF))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhum aluno em aula nesta sessão.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            activeLessons.forEach { aula ->
                val al = allAlunos.find { it.id == aula.alunoId }
                val moto = allMotos.find { it.id == aula.motoId }
                
                // Calculate individual timer elapsed
                val elapsedSec = (currentTimeMillis - aula.dataHoraInicio) / 1000
                val totalSec = aula.duracaoMinutos * 60
                val remainingSec = (totalSec - elapsedSec).coerceAtLeast(0L)
                
                val remMins = remainingSec / 60
                val remSecs = remainingSec % 60
                
                val timerFormatted = String.format(Locale.getDefault(), "%02d:%02d", remMins, remSecs)
                val isTimeOver = remainingSec == 0L

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, if (isTimeOver) Color.Red.copy(alpha = 0.3f) else Color(0xFFEFEFEF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = al?.nome ?: "Aluno Desconhecido",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isTimeOver) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Tempo Esgotado",
                                            tint = Color.Red,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { viewModel.startIndividualCheckout(aula.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = "Tirar fotos de finalização",
                                            tint = OrangeAutoescola,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Moto: ${moto?.marca ?: ""} ${moto?.modelo ?: "Honda CG"} (${moto?.placa ?: "AAA-0000"})",
                                    fontSize = 13.sp,
                                    color = DarkGrey
                                )
                            }
                            
                            // Timer badge and status
                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isTimeOver) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = timerFormatted,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = if (isTimeOver) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                                    )
                                }
                                if (isTimeOver) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Pendente de Finalização",
                                        fontSize = 11.sp,
                                        color = Color(0xFFD32F2F),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions Row: Checkout & Add Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startIndividualCheckout(aula.id) },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OrangeAutoescola
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Checkout / Fotos",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            val maxExtra = 120 - aula.duracaoMinutos
                            if (maxExtra > 0) {
                                var showAddTimeDialog by remember { mutableStateOf(false) }
                                
                                OutlinedButton(
                                    onClick = { showAddTimeDialog = true },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeAutoescola),
                                    border = BorderStroke(1.dp, OrangeAutoescola.copy(alpha = 0.5f))
                                ) {
                                    Icon(imageVector = Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("+ Tempo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                if (showAddTimeDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showAddTimeDialog = false },
                                        title = { Text("Adicionar Tempo de Aula", fontWeight = FontWeight.Bold) },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Text("Selecione quantos minutos deseja acrescentar para ${al?.nome ?: "o aluno"}. O limite máximo por aula é de 2 horas (120 minutos).", fontSize = 14.sp)
                                                Text("Duração atual: ${aula.duracaoMinutos} min. (Pode adicionar até $maxExtra min.)", fontSize = 13.sp, color = Color.Gray)
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    listOf(15, 30, 45, 60).forEach { mins ->
                                                        if (mins <= maxExtra) {
                                                            Button(
                                                                onClick = {
                                                                    viewModel.addTimeToLesson(aula.id, mins)
                                                                    showAddTimeDialog = false
                                                                },
                                                                modifier = Modifier.weight(1f),
                                                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                                            ) {
                                                                Text("+$mins min", fontSize = 12.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        confirmButton = {},
                                        dismissButton = {
                                            TextButton(onClick = { showAddTimeDialog = false }) {
                                                Text("Cancelar", color = Color.Gray)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        var showFinalizeSessionConfirm by remember { mutableStateOf(false) }

        Button(
            onClick = { showFinalizeSessionConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), // Elegant Material Red
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Finalizar Sessão de Aulas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        if (showFinalizeSessionConfirm) {
            if (activeLessons.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { showFinalizeSessionConfirm = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Atenção",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Não é Possível Finalizar a Sessão",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Text(
                            text = "Atenção: Existem ${activeLessons.size} aluno(s) com aulas em andamento ou atividades pendentes no fluxo (fotos, etapas ou checkout). Conclua a aula de cada aluno individualmente antes de encerrar a sessão.",
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showFinalizeSessionConfirm = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("Entendido", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            } else {
                AlertDialog(
                    onDismissRequest = { showFinalizeSessionConfirm = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Confirmar",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Encerrar Sessão Atual?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Text(
                            text = "Deseja realmente finalizar a sessão de aulas atual? Todos os alunos já concluíram suas respectivas aulas com sucesso!",
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showFinalizeSessionConfirm = false
                                viewModel.finalizeFullSession()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("Sim, Finalizar", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showFinalizeSessionConfirm = false }) {
                            Text("Cancelar", color = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}

private fun copyUriToFile(context: android.content.Context, uri: Uri, destFile: File): Boolean {
    return com.example.util.FileHelper.copyUriToFile(context, uri, destFile)
}
