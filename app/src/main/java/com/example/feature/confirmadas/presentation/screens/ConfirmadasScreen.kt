package com.example.feature.confirmadas.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.BorderStroke
import coil.compose.AsyncImage
import com.example.core.database.dao.AulaWithDetails
import com.example.ui.components.ExportedFilesDialog
import com.example.ui.theme.OrangeAutoescola
import com.example.feature.confirmadas.presentation.ConfirmadasViewModel
import com.example.util.FileHelper
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmadasScreen(viewModel: ConfirmadasViewModel) {
    val allAulas by viewModel.allAulas.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showFilesDialog by remember { mutableStateOf(false) }

    // Filters / Search
    var searchQuery by remember { mutableStateOf("") }
    var filterPendingOnly by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf("Tudo") } // "Tudo", "Hoje", "7 Dias", "30 Dias"

    // 1. Date filter (based on dataHoraFim)
    val now = System.currentTimeMillis()
    val dateFilteredAulas = allAulas.filter { a ->
        if (a.statusAula == "cancelada") return@filter false
        if (a.dataHoraFim == 0L) {
            // Include pending only if selection is "Tudo"
            return@filter selectedPeriod == "Tudo"
        }
        
        when (selectedPeriod) {
            "Hoje" -> {
                val calNow = Calendar.getInstance()
                val calAula = Calendar.getInstance().apply { timeInMillis = a.dataHoraFim }
                calNow.get(Calendar.YEAR) == calAula.get(Calendar.YEAR) &&
                calNow.get(Calendar.DAY_OF_YEAR) == calAula.get(Calendar.DAY_OF_YEAR)
            }
            "7 Dias" -> {
                a.dataHoraFim >= now - 7L * 24 * 60 * 60 * 1000
            }
            "30 Dias" -> {
                a.dataHoraFim >= now - 30L * 24 * 60 * 60 * 1000
            }
            else -> true
        }
    }

    // 2. Text Search & Pending Sync filter
    val filteredAulas = dateFilteredAulas.filter { a ->
        val matchesSearch = a.alunoNome.contains(searchQuery, ignoreCase = true) ||
                a.motoModelo.contains(searchQuery, ignoreCase = true)
        val matchesPending = !filterPendingOnly || a.statusAula == "pendente"
        matchesSearch && matchesPending
    }

    // Statistics calculations
    val totalAulas = filteredAulas.size
    val totalKm = filteredAulas.sumOf { it.kmPercorrido }
    val uniqueAlunos = filteredAulas.map { it.alunoId }.distinct().size
    val uniqueMotos = filteredAulas.map { it.motoId }.distinct().size

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Aulas ", fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                        Text("Confirmadas", fontWeight = FontWeight.Light, color = OrangeAutoescola)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showFilesDialog = true }
                    ) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = "Abrir pasta de relatórios", tint = OrangeAutoescola)
                    }
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val file = viewModel.exportAllDataToDownloads()
                                if (file != null) {
                                    FileHelper.shareFile(context, file)
                                } else {
                                    Toast.makeText(context, "Falha ao exportar", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Compartilhar tudo", tint = OrangeAutoescola)
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
        ) {
            // Dashboard Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7F2)),
                border = BorderStroke(1.dp, Color(0xFFFBE9E1))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Dashboard de Estatísticas (${selectedPeriod})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeAutoescola,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.Timeline, contentDescription = null, tint = OrangeAutoescola, modifier = Modifier.size(20.dp))
                                    Text("Aulas", fontSize = 10.sp, color = Color.Gray)
                                    Text("$totalAulas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = OrangeAutoescola, modifier = Modifier.size(20.dp))
                                    Text("KMs Rodados", fontSize = 10.sp, color = Color.Gray)
                                    Text("${totalKm}km", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.People, contentDescription = null, tint = OrangeAutoescola, modifier = Modifier.size(20.dp))
                                    Text("Alunos", fontSize = 10.sp, color = Color.Gray)
                                    Text("$uniqueAlunos", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = Icons.Default.TwoWheeler, contentDescription = null, tint = OrangeAutoescola, modifier = Modifier.size(20.dp))
                                    Text("Motos", fontSize = 10.sp, color = Color.Gray)
                                    Text("$uniqueMotos", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                                }
                            }
                        }
                    }
                }
            }

            // Filters Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar por aluno ou moto...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = OrangeAutoescola) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeAutoescola,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Period selector Chips (Date Filter!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Tudo", "Hoje", "7 Dias", "30 Dias").forEach { period ->
                            val isSelected = selectedPeriod == period
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) OrangeAutoescola else Color(0xFFF5F5F5))
                                    .clickable { selectedPeriod = period }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = period,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else Color.DarkGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { filterPendingOnly = !filterPendingOnly }
                        ) {
                            Checkbox(
                                checked = filterPendingOnly,
                                onCheckedChange = { filterPendingOnly = it },
                                colors = CheckboxDefaults.colors(checkedColor = OrangeAutoescola)
                            )
                            Text("Apenas pendentes de sincronização", fontSize = 11.sp, color = Color(0xFF1C1B1F))
                        }

                        // Export Options (ZIP & JSON Export!)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // JSON Export
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val file = viewModel.exportPeriodDataToJson(selectedPeriod)
                                        if (file != null) {
                                            FileHelper.shareFile(context, file)
                                        } else {
                                            Toast.makeText(context, "Sem dados ou erro no JSON", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Description, contentDescription = "JSON", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("JSON", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            // ZIP Export
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val file = viewModel.exportAllDataToDownloads()
                                        if (file != null) {
                                            FileHelper.shareFile(context, file)
                                        } else {
                                            Toast.makeText(context, "Falha ao gerar ZIP", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Archive, contentDescription = "Zip", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ZIP", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Results List
            if (filteredAulas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Empty",
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Nenhuma aula confirmada",
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "As aulas concluídas e validadas aparecerão listadas aqui.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp, start = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAulas) { aula ->
                        AulaListItem(aula = aula, viewModel = viewModel)
                    }
                }
            }
        }
    }

    if (showFilesDialog) {
        ExportedFilesDialog(onDismiss = { showFilesDialog = false })
    }
}

@Composable
fun AulaListItem(aula: AulaWithDetails, viewModel: ConfirmadasViewModel) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val stf = SimpleDateFormat("HH:mm", Locale.getDefault())

    val dateStr = sdf.format(Date(aula.dataHoraInicio))
    val timeStr = "${stf.format(Date(aula.dataHoraInicio))} - ${stf.format(Date(aula.dataHoraFim))}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.openPdfForHistoricalSession(aula.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Student Profile Circle
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEEEEE))
                    .border(2.dp, OrangeAutoescola, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (aula.alunoFoto.isNotEmpty()) {
                    AsyncImage(
                        model = File(aula.alunoFoto),
                        contentDescription = "Aluno profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "Aluno", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = aula.alunoNome,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Date", modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = dateStr, fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = "Time", modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = timeStr, fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.TwoWheeler, contentDescription = "Moto", modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${aula.motoModelo} [${aula.motoPlaca}]", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(imageVector = Icons.Default.Pin, contentDescription = "KM", modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${aula.kmPercorrido} km rodados", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Sync status column
            Column(horizontalAlignment = Alignment.End) {
                val isPending = aula.statusAula == "pendente"

                SuggestionChip(
                    onClick = { viewModel.toggleSyncFlag(aula.id) },
                    label = {
                        Text(
                            text = if (isPending) "Pendente" else "Sincronizada",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        labelColor = if (isPending) Color(0xFFFF9800) else Color(0xFF4CAF50),
                        containerColor = if (isPending) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "Open PDF",
                    tint = OrangeAutoescola,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
