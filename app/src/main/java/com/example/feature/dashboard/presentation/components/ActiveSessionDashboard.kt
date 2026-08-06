package com.example.feature.dashboard.presentation.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.feature.dashboard.presentation.DashboardUiEvent
import com.example.feature.dashboard.presentation.DashboardViewModel
import com.example.ui.theme.DarkGrey
import com.example.ui.theme.OrangeAutoescola
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionDashboard(
    dashboardViewModel: DashboardViewModel,
    onRefreshTimer: () -> Unit,
    onStartAddingStudent: () -> Unit,
    onStartIndividualCheckout: (Long) -> Unit,
    onAddTimeToLesson: (Long, Int) -> Unit,
    onFinalizeFullSession: () -> Unit
) {
    val uiState by dashboardViewModel.uiState.collectAsState()
    val activeLessons = uiState.activeLessons
    val allAlunos = uiState.allAlunos
    val allMotos = uiState.allMotos
    val currentInstrutor = uiState.currentInstrutor
    val currentTimeMillis = uiState.currentTimeMillis

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                onRefreshTimer()
                dashboardViewModel.onEvent(DashboardUiEvent.RefreshTimerOnResume)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(Unit) {
        onRefreshTimer()
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
        val completedToday = uiState.completedTodayCount

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
        val pendingCount = uiState.pendingBackupCount
        val lastSyncTime = uiState.lastBackupTime
        val isBackupEnabled = uiState.isGoogleBackupEnabled && !uiState.googleAccountEmail.isNullOrEmpty()

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
            onClick = { onStartAddingStudent() },
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
                        val isCheckoutAvailable = aula.dataHoraFim > 0L || isTimeOver
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
                                    if (isCheckoutAvailable) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Tempo Esgotado",
                                            tint = Color.Red,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { onStartIndividualCheckout(aula.id) },
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
                                onClick = { onStartIndividualCheckout(aula.id) },
                                enabled = isCheckoutAvailable,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCheckoutAvailable) OrangeAutoescola else Color.Gray,
                                    disabledContainerColor = Color(0xFFE0E0E0),
                                    disabledContentColor = Color.Gray
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCheckoutAvailable) "Checkout / Fotos" else "Aula em Andamento",
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
                                                                    onAddTimeToLesson(aula.id, mins)
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
                                onFinalizeFullSession()
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
