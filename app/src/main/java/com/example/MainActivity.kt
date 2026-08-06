package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import com.example.feature.agenda.presentation.screens.AgendaScreen
import com.example.feature.agenda.presentation.AgendaViewModel
import com.example.feature.cadastros.presentation.CadastrosViewModel
import com.example.feature.cadastros.presentation.screens.CadastrosScreen
import com.example.feature.aula.presentation.viewmodel.AulaViewModel
import com.example.feature.confirmadas.presentation.ConfirmadasViewModel
import com.example.feature.configuracoes.presentation.ConfiguracoesViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.feature.configuracoes.presentation.screens.AjustesScreen
import com.example.feature.aula.presentation.screens.AulaScreen
import com.example.feature.confirmadas.presentation.screens.ConfirmadasScreen
import com.example.ui.components.UpdateAvailableDialog
import com.example.feature.dashboard.presentation.components.ActiveSessionDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OrangeAutoescola
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.example.license.LicenseManager
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.shape.CircleShape
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val licenseManager = LicenseManager(applicationContext)
        setContent {
            MyApplicationTheme {
                val initialInfo = remember { licenseManager.checkLicense() }
                var currentStatus by remember { mutableStateOf(initialInfo.status) }
                var currentErrorMessage by remember { mutableStateOf(initialInfo.errorMessage) }
                var downloadUrl by remember { mutableStateOf(initialInfo.downloadUrl) }
                var pendingUpdate by remember { mutableStateOf(licenseManager.getPendingUpdate()) }
                when (currentStatus) {
                    LicenseManager.LicenseStatus.FORCE_UPDATE -> ForceUpdateScreen(downloadUrl = downloadUrl ?: "")
                    LicenseManager.LicenseStatus.ACTIVE -> {
                        MainAppContainer()
                        pendingUpdate?.let { info ->
                            UpdateAvailableDialog(
                                licenseManager = licenseManager,
                                latestVersionName = info.latestVersionName,
                                latestVersionCode = info.latestVersionCode,
                                releaseNotes = info.releaseNotes,
                                downloadUrl = info.downloadUrl ?: "",
                                onDismiss = {
                                    pendingUpdate = null
                                    licenseManager.clearPendingUpdate()
                                }
                            )
                        }
                    }
                    LicenseManager.LicenseStatus.NO_LICENSE -> {
                        ActivationScreen(licenseManager) { info ->
                            currentStatus = info.status
                            currentErrorMessage = info.errorMessage
                            downloadUrl = info.downloadUrl
                            pendingUpdate = licenseManager.getPendingUpdate()
                        }
                    }
                    else -> BlockedScreen(
                        title = when(currentStatus) { LicenseManager.LicenseStatus.EXPIRED -> "Avaliação Expirada"; LicenseManager.LicenseStatus.BLOCKED -> "Licença Bloqueada"; else -> "Servidor Indisponível" },
                        message = currentErrorMessage ?: "Contate o suporte",
                        onRetry = {
                            val info = licenseManager.checkLicense()
                            currentStatus = info.status
                            currentErrorMessage = info.errorMessage
                            downloadUrl = info.downloadUrl
                            pendingUpdate = licenseManager.getPendingUpdate()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppContainer() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val agendaViewModel: AgendaViewModel = viewModel(factory = AgendaViewModel.Factory(context.applicationContext))
    val cadastrosViewModel: CadastrosViewModel = viewModel(factory = CadastrosViewModel.Factory(context.applicationContext))
    val aulaViewModel: AulaViewModel = viewModel(factory = AulaViewModel.Factory(context.applicationContext as android.app.Application))
    val confirmadasViewModel: ConfirmadasViewModel = viewModel(factory = ConfirmadasViewModel.Factory(context.applicationContext as android.app.Application))
    val configuracoesViewModel: ConfiguracoesViewModel = viewModel(factory = ConfiguracoesViewModel.Factory(context.applicationContext as android.app.Application))
    val dashboardViewModel: com.example.feature.dashboard.presentation.DashboardViewModel = viewModel(factory = com.example.feature.dashboard.presentation.DashboardViewModel.Factory(context.applicationContext as android.app.Application))

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = OrangeAutoescola,
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = Color(0xFFE0E0E0),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            ) {
                val items = listOf(
                    NavigationItem("aula", "Aula", Icons.Default.SportsMotorsports),
                    NavigationItem("agenda", "Agenda", Icons.Default.DateRange),
                    NavigationItem("confirmadas", "Confirmadas", Icons.Default.CheckCircle),
                    NavigationItem("cadastros", "Cadastros", Icons.Default.Assignment),
                    NavigationItem("ajustes", "Ajustes", Icons.Default.Settings)
                )

                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                        label = { Text(item.title, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OrangeAutoescola,
                            selectedTextColor = OrangeAutoescola,
                            indicatorColor = OrangeAutoescola.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "aula",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("aula") {
                AulaScreen(
                    viewModel = aulaViewModel
                )
            }
            composable("agenda") {
                AgendaScreen(
                    viewModel = agendaViewModel,
                    onStartAula = { alunoId, agendamentoId ->
                        aulaViewModel.startClassFromAgenda(alunoId, agendamentoId)
                        navController.navigate("aula") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable("confirmadas") {
                ConfirmadasScreen(viewModel = confirmadasViewModel)
            }
            composable("cadastros") {
                CadastrosScreen(viewModel = cadastrosViewModel)
            }
            composable("ajustes") {
                AjustesScreen(viewModel = configuracoesViewModel)
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

class LicenseKeyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val formatted = StringBuilder()
        for (i in originalText.indices) {
            if (i > 0 && i % 4 == 0) {
                formatted.append('-')
            }
            formatted.append(originalText[i])
        }
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val mapped = when {
                    offset <= 4 -> offset
                    offset <= 8 -> offset + 1
                    offset <= 12 -> offset + 2
                    else -> offset + 3
                }
                return mapped.coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val mapped = when {
                    offset <= 4 -> offset
                    offset <= 9 -> offset - 1
                    offset <= 14 -> offset - 2
                    else -> offset - 3
                }
                return mapped.coerceIn(0, originalText.length)
            }
        }
        
        return TransformedText(AnnotatedString(formatted.toString()), offsetMapping)
    }
}

private fun formatLicenseKey(input: String): String {
    val clean = input.filter { it.isLetterOrDigit() }.uppercase().take(16)
    val sb = StringBuilder()
    for (i in clean.indices) {
        if (i > 0 && i % 4 == 0) {
            sb.append('-')
        }
        sb.append(clean[i])
    }
    return sb.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationScreen(licenseManager: LicenseManager, onActivated: (LicenseManager.LicenseInfo) -> Unit) {
    val scope = rememberCoroutineScope()
    var licenseKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var deviceUuid by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { deviceUuid = licenseManager.getDeviceUuid().take(16) + "..." }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(OrangeAutoescola), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Text("ValidaMoto", fontSize = 28.sp, fontWeight = FontWeight.Black, color = OrangeAutoescola)
            Text("Ativação de Licença", fontSize = 16.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ID do Dispositivo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(deviceUuid, fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(
                value = licenseKey,
                onValueChange = { input ->
                    val clean = input.filter { it.isLetterOrDigit() }.uppercase().take(16)
                    licenseKey = clean
                    errorMessage = null
                },
                label = { Text("License Key") },
                placeholder = { Text("XXXX-XXXX-XXXX-XXXX") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                visualTransformation = LicenseKeyVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = OrangeAutoescola,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = OrangeAutoescola,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            errorMessage?.let { msg -> Text(msg, fontSize = 13.sp, color = Color.Red) }
            Button(
                onClick = {
                    if (licenseKey.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val formattedKey = formatLicenseKey(licenseKey)
                            val result = licenseManager.activate(formattedKey)
                            isLoading = false
                            result.fold({ info -> onActivated(info) }, { e -> errorMessage = e.message })
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading && licenseKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                else { Icon(Icons.Default.Check, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Ativar Licença", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ForceUpdateScreen(downloadUrl: String) {
    val context = LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun startDownloadAndInstall() {
        if (downloadUrl.isBlank()) {
            errorMessage = "URL de download não informada pelo servidor."
            return
        }
        try {
            isDownloading = true
            errorMessage = null

            val fileName = "ValidMoto_Update.apk"
            val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("ValidMoto")
                .setDescription("Baixando atualização do aplicativo...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destinationFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)

            val onCompleteReceiver = object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        isDownloading = false
                        try {
                            context.unregisterReceiver(this)
                        } catch (e: Exception) {}

                        try {
                            val apkUri: Uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                destinationFile
                            )
                            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(apkUri, "application/vnd.android.package-archive")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            context.startActivity(installIntent)
                        } catch (e: Exception) {
                            Log.e("ForceUpdateScreen", "Erro ao abrir instalador: ${e.message}", e)
                            errorMessage = "Erro ao abrir instalador. Verifique se o app tem permissão para instalar apps desconhecidos."
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onCompleteReceiver,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            isDownloading = false
            errorMessage = "Erro ao iniciar download: ${e.message}"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF3E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = OrangeAutoescola,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Atualização Obrigatória",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Uma nova versão do aplicativo está disponível. Você precisa atualizar para continuar usando.",
                fontSize = 15.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isDownloading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = OrangeAutoescola)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Baixando atualização...",
                        fontSize = 14.sp,
                        color = OrangeAutoescola,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Button(
                    onClick = { startDownloadAndInstall() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Baixar Atualização",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun BlockedScreen(title: String, message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Red, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Red, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola), shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Tentar Novamente", fontWeight = FontWeight.Bold)
        }
    }
}
