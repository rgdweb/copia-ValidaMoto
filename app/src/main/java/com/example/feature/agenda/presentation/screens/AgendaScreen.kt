package com.example.feature.agenda.presentation.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.database.entity.Aluno
import com.example.core.database.entity.Moto
import com.example.core.database.dao.AgendamentoWithDetails
import com.example.ui.theme.OrangeAutoescola
import com.example.feature.agenda.presentation.AgendaViewModel
import com.example.feature.agenda.presentation.AgendaUiEvent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    viewModel: AgendaViewModel,
    onStartAula: ((alunoId: Long, agendamentoId: Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showScheduleDialog by remember { mutableStateOf(false) }
    var editingAgendamento by remember { mutableStateOf<AgendamentoWithDetails?>(null) }
    var selectedFilter by remember { mutableStateOf("Todos") }
    var itemToDeleteId by remember { mutableStateOf<Long?>(null) }

    val filteredAgendamentos = remember(uiState.agendamentos, selectedFilter) {
        uiState.agendamentos
            .filter { item ->
                when (selectedFilter) {
                    "Hoje" -> isToday(item.dataHora)
                    "Agendadas" -> item.status == "agendada"
                    "Realizadas" -> item.status == "realizada"
                    else -> true
                }
            }
            .sortedBy { it.dataHora }
    }

    val groupedAgendamentos = remember(filteredAgendamentos) {
        filteredAgendamentos.groupBy { formatDayHeader(it.dataHora) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Agenda de Aulas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            "Planeje e organize os horários de treinos",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangeAutoescola,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState.alunos.isEmpty()) {
                        Toast.makeText(context, "Cadastre alunos antes de agendar", Toast.LENGTH_LONG).show()
                    } else if (uiState.motos.isEmpty()) {
                        Toast.makeText(context, "Cadastre motos antes de agendar", Toast.LENGTH_LONG).show()
                    } else {
                        editingAgendamento = null
                        showScheduleDialog = true
                    }
                },
                containerColor = OrangeAutoescola,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_schedule_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Agendar Aula")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F9FC))
                .padding(innerPadding)
        ) {
            // Stats Header Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(OrangeAutoescola.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Agenda",
                                tint = OrangeAutoescola
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Total de Aulas Agendadas",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${uiState.agendamentos.size} aulas",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                    
                    val activeCount = uiState.agendamentos.count { it.status == "agendada" }
                    Badge(
                        containerColor = OrangeAutoescola.copy(alpha = 0.15f),
                        contentColor = OrangeAutoescola,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            "$activeCount Pendentes",
                            modifier = Modifier.padding(4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Filters
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterOptions = listOf("Todos", "Hoje", "Agendadas", "Realizadas")
                items(filterOptions) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeAutoescola,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF475569)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color(0xFFE2E8F0),
                            selectedBorderColor = OrangeAutoescola,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            if (filteredAgendamentos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Sem compromissos",
                            modifier = Modifier.size(72.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Nenhuma aula encontrada.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF64748B)
                        )
                        if (uiState.agendamentos.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Toque no botão '+' abaixo para agendar o primeiro treino dos seus alunos.",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    groupedAgendamentos.forEach { (dayHeader, itemsInDay) ->
                        item(key = "header_$dayHeader") {
                            Text(
                                text = dayHeader,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(itemsInDay, key = { it.id }) { item ->
                            AgendaItemCard(
                                item = item,
                                onStatusChange = { id, status ->
                                    viewModel.onEvent(AgendaUiEvent.UpdateStatus(id, status))
                                },
                                onEdit = { itemToEdit ->
                                    editingAgendamento = itemToEdit
                                    showScheduleDialog = true
                                },
                                onDelete = { id ->
                                    itemToDeleteId = id
                                },
                                onStartAula = onStartAula
                            )
                        }
                    }
                }
            }
        }

        if (showScheduleDialog) {
            ScheduleClassDialog(
                alunos = uiState.alunos,
                motos = uiState.motos,
                initialAgendamento = editingAgendamento,
                onDismiss = {
                    showScheduleDialog = false
                    editingAgendamento = null
                },
                onSchedule = { alunoId, motoId, timestamp, obs, id ->
                    viewModel.onEvent(AgendaUiEvent.ScheduleClass(alunoId, motoId, timestamp, obs, id))
                    showScheduleDialog = false
                    editingAgendamento = null
                    val successMsg = if (id == 0L) "Aula agendada com sucesso!" else "Agendamento atualizado com sucesso!"
                    Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (itemToDeleteId != null) {
            AlertDialog(
                onDismissRequest = { itemToDeleteId = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Confirmar Exclusão",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Excluir Agendamento?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "Esta ação é irreversível e removerá permanentemente o agendamento selecionado da agenda.",
                        fontSize = 14.sp,
                        color = Color(0xFF475569)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = itemToDeleteId
                            itemToDeleteId = null
                            if (id != null) {
                                viewModel.onEvent(AgendaUiEvent.DeleteSchedule(id))
                                Toast.makeText(context, "Agendamento excluído com sucesso.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Excluir", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { itemToDeleteId = null }
                    ) {
                        Text("Cancelar", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun AgendaItemCard(
    item: AgendamentoWithDetails,
    onStatusChange: (Long, String) -> Unit,
    onEdit: (AgendamentoWithDetails) -> Unit,
    onDelete: (Long) -> Unit,
    onStartAula: ((alunoId: Long, agendamentoId: Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault()) }
    val formattedDate = sdf.format(Date(item.dataHora))

    val isExame = item.tipo == "EXAME"

    val statusColor = when (item.status) {
        "agendada" -> if (isExame) Color(0xFF9333EA) else Color(0xFF3B82F6) // Purple for EXAME, Blue for AULA
        "realizada" -> Color(0xFF10B981) // Green
        "cancelada" -> Color(0xFFEF4444) // Red
        else -> Color.Gray
    }

    val statusLabel = when (item.status) {
        "agendada" -> "Agendada"
        "realizada" -> "Realizada"
        "cancelada" -> "Cancelada"
        else -> item.status.replaceFirstChar { it.uppercase() }
    }

    val typeLabel = if (isExame) "EXAME DETRAN" else "AULA"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("agenda_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Date and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Horário",
                        tint = OrangeAutoescola,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        formattedDate,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF334155)
                    )
                }

                // Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(typeLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("•", color = Color.Gray, fontSize = 11.sp)
                        Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))

            // Student and Motorcycle Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isExame) Color(0xFFF3E8FF) else Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExame) Icons.Default.School else Icons.Default.Person,
                        contentDescription = if (isExame) "Exame" else "Aluno",
                        tint = if (isExame) Color(0xFF9333EA) else Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.alunoNome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B)
                    )
                    if (isExame) {
                        Text(
                            "Exame prático do DETRAN",
                            fontSize = 12.sp,
                            color = Color(0xFF9333EA),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBike,
                                contentDescription = "Moto",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${item.motoModelo ?: ""} • PLACA ${item.motoPlaca ?: "—"}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            if (item.observacoes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        "Obs: ${item.observacoes}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExame) {
                    // EXAME: somente compartilhar (edicao/remocao via cadastro do aluno)
                    Text(
                        "Gerenciado pelo cadastro do aluno",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(item.dataHora))
                            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.dataHora))
                            val messageBuilder = StringBuilder()
                            messageBuilder.append("📅 *Lembrete de Exame DETRAN*\n\n")
                            messageBuilder.append("Olá, ${item.alunoNome}!\n\n")
                            messageBuilder.append("Este é um lembrete do seu exame prático.\n\n")
                            messageBuilder.append("📆 Data: $dateFmt\n")
                            messageBuilder.append("🕒 Horário: $timeFmt\n")
                            messageBuilder.append("\nBoa sorte! Em caso de dúvidas, entre em contato com seu instrutor.")
                            val message = messageBuilder.toString()

                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, message)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Compartilhar exame")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.testTag("share_exame_button_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar Exame",
                            tint = Color(0xFF9333EA)
                        )
                    }
                } else {
                    // AULA: Edit, Delete & Share buttons (comportamento original)
                    Row {
                        IconButton(
                            onClick = { onEdit(item) },
                            modifier = Modifier.testTag("edit_schedule_button_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Agendamento",
                                tint = Color(0xFF64748B)
                            )
                        }
                        IconButton(
                            onClick = { onDelete(item.id) },
                            modifier = Modifier.testTag("delete_schedule_button_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir Agendamento",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                        IconButton(
                            onClick = {
                                val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(item.dataHora))
                                val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.dataHora))
                                val messageBuilder = StringBuilder()
                                messageBuilder.append("📅 *Lembrete de Aula*\n\n")
                                messageBuilder.append("Olá, ${item.alunoNome}!\n\n")
                                messageBuilder.append("Este é um lembrete da sua aula prática.\n\n")
                                messageBuilder.append("📆 Data: $dateFmt\n")
                                messageBuilder.append("🕒 Horário: $timeFmt\n")
                                if (item.observacoes.isNotBlank()) {
                                    messageBuilder.append("📍 Local: ${item.observacoes}\n")
                                }
                                messageBuilder.append("\nEm caso de necessidade, entre em contato com seu instrutor.")
                                val message = messageBuilder.toString()

                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, message)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Compartilhar agendamento")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.testTag("share_schedule_button_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartilhar Agendamento",
                                tint = Color(0xFF3B82F6)
                            )
                        }
                    }
                }

                Row {
                    if (item.status == "agendada") {
                        OutlinedButton(
                            onClick = { onStatusChange(item.id, "cancelada") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(36.dp)
                                .testTag("cancel_schedule_button_${item.id}"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("Cancelar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (!isExame) {
                            // Botao "Realizar" somente para AULA (nao para EXAME - exame nao inicia sessao de aula)
                            Button(
                                onClick = {
                                    if (onStartAula != null) {
                                        onStartAula(item.alunoId, item.id)
                                    } else {
                                        onStatusChange(item.id, "realizada")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("realize_schedule_button_${item.id}"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("Realizar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            // EXAME: botao para marcar como realizada sem iniciar sessao
                            Button(
                                onClick = { onStatusChange(item.id, "realizada") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("complete_exame_button_${item.id}"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Text("Concluir", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else if (item.status == "cancelada" || item.status == "realizada") {
                        // Reset back to agendada
                        OutlinedButton(
                            onClick = { onStatusChange(item.id, "agendada") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("reopen_schedule_button_${item.id}"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text("Reagendar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleClassDialog(
    alunos: List<Aluno>,
    motos: List<Moto>,
    initialAgendamento: AgendamentoWithDetails? = null,
    onDismiss: () -> Unit,
    onSchedule: (alunoId: Long, motoId: Long, timestamp: Long, observacoes: String, id: Long) -> Unit
) {
    var selectedAlunoId by remember { mutableStateOf(initialAgendamento?.alunoId ?: alunos.firstOrNull()?.id ?: -1L) }
    var selectedMotoId by remember { mutableStateOf(initialAgendamento?.motoId ?: motos.firstOrNull()?.id ?: -1L) }

    val context = LocalContext.current

    val initialCalendar = remember {
        Calendar.getInstance().apply {
            if (initialAgendamento != null) {
                timeInMillis = initialAgendamento.dataHora
            }
        }
    }

    var selectedYear by remember { mutableStateOf(initialCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(initialCalendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(initialCalendar.get(Calendar.DAY_OF_MONTH)) }

    var startHour by remember { mutableStateOf(initialCalendar.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableStateOf(initialCalendar.get(Calendar.MINUTE)) }

    var endHour by remember { mutableStateOf((startHour + 1) % 24) }
    var endMinute by remember { mutableStateOf(startMinute) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedYear = year
                selectedMonth = month
                selectedDay = dayOfMonth
            },
            selectedYear,
            selectedMonth,
            selectedDay
        )
    }

    val startTimePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                startHour = hourOfDay
                startMinute = minute
                if (endHour < hourOfDay || (endHour == hourOfDay && endMinute <= minute)) {
                    endHour = (hourOfDay + 1) % 24
                    endMinute = minute
                }
            },
            startHour,
            startMinute,
            true
        )
    }

    val endTimePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                endHour = hourOfDay
                endMinute = minute
            },
            endHour,
            endMinute,
            true
        )
    }

    val dateStr = remember(selectedYear, selectedMonth, selectedDay) {
        val cal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, selectedDay)
        }
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
    }

    val startTimeStr = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMinute)
    val endTimeStr = String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute)

    val startTotalMinutes = startHour * 60 + startMinute
    val endTotalMinutes = endHour * 60 + endMinute
    val diffMinutes = endTotalMinutes - startTotalMinutes

    val numAulas = if (diffMinutes > 0) Math.round(diffMinutes.toDouble() / 60.0).toInt() else 0

    var alunoExpanded by remember { mutableStateOf(false) }
    var motoExpanded by remember { mutableStateOf(false) }

    val currentSelectedAluno = alunos.find { it.id == selectedAlunoId } ?: alunos.firstOrNull()
    val currentSelectedMoto = motos.find { it.id == selectedMotoId } ?: motos.firstOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    if (initialAgendamento == null) "Novo Agendamento" else "Editar Agendamento",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Aluno", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = alunoExpanded,
                    onExpandedChange = { alunoExpanded = !alunoExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentSelectedAluno?.nome ?: "Selecione o Aluno",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = alunoExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeAutoescola,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = alunoExpanded,
                        onDismissRequest = { alunoExpanded = false }
                    ) {
                        alunos.forEach { aluno ->
                            DropdownMenuItem(
                                text = { Text(aluno.nome) },
                                onClick = {
                                    selectedAlunoId = aluno.id
                                    alunoExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Veículo (Moto)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = motoExpanded,
                    onExpandedChange = { motoExpanded = !motoExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentSelectedMoto?.let { "${it.modelo} (Placa: ${it.placa})" } ?: "Selecione a Moto",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = motoExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeAutoescola,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = motoExpanded,
                        onDismissRequest = { motoExpanded = false }
                    ) {
                        motos.forEach { moto ->
                            DropdownMenuItem(
                                text = { Text("${moto.modelo} (${moto.placa})") },
                                onClick = {
                                    selectedMotoId = moto.id
                                    motoExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Data", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color(0xFF1E293B),
                        disabledBorderColor = Color(0xFFCBD5E1),
                        disabledTrailingIconColor = OrangeAutoescola
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Selecionar data",
                            modifier = Modifier.clickable { datePickerDialog.show() }
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Horário Inicial", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = startTimeStr,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { startTimePickerDialog.show() },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color(0xFF1E293B),
                                disabledBorderColor = Color(0xFFCBD5E1),
                                disabledTrailingIconColor = OrangeAutoescola
                            ),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Selecionar horário inicial",
                                    modifier = Modifier.clickable { startTimePickerDialog.show() }
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Horário Final", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = endTimeStr,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { endTimePickerDialog.show() },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color(0xFF1E293B),
                                disabledBorderColor = Color(0xFFCBD5E1),
                                disabledTrailingIconColor = OrangeAutoescola
                            ),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Selecionar horário final",
                                    modifier = Modifier.clickable { endTimePickerDialog.show() }
                                )
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Nº de aulas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = if (diffMinutes > 0) "$numAulas aula(s)" else "Horário inválido",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (diffMinutes > 0) Color(0xFF1E293B) else Color(0xFFEF4444),
                        disabledBorderColor = Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancelar", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            if (endTotalMinutes <= startTotalMinutes) {
                                Toast.makeText(
                                    context,
                                    "O horário final deve ser maior que o horário inicial.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            val finalCalendar = Calendar.getInstance().apply {
                                set(Calendar.YEAR, selectedYear)
                                set(Calendar.MONTH, selectedMonth)
                                set(Calendar.DAY_OF_MONTH, selectedDay)
                                set(Calendar.HOUR_OF_DAY, startHour)
                                set(Calendar.MINUTE, startMinute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            val obsFormatted = "$startTimeStr às $endTimeStr ($numAulas aula(s))"
                            onSchedule(
                                selectedAlunoId,
                                selectedMotoId,
                                finalCalendar.timeInMillis,
                                obsFormatted,
                                initialAgendamento?.id ?: 0L
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (initialAgendamento == null) "Confirmar" else "Salvar", color = Color.White)
                    }
                }
            }
        }
    }
}

private fun isToday(timestamp: Long): Boolean {
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    return target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
           target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
}

private fun formatDayHeader(timestamp: Long): String {
    val dayMonthFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    val fullDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val calTodayAtMidnight = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val calTargetAtMidnight = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val diffMillis = calTargetAtMidnight.timeInMillis - calTodayAtMidnight.timeInMillis
    val diffDays = Math.round(diffMillis.toDouble() / (24 * 60 * 60 * 1000)).toInt()

    return when (diffDays) {
        0 -> "HOJE - ${dayMonthFormat.format(Date(timestamp))}"
        1 -> "AMANHÃ - ${dayMonthFormat.format(Date(timestamp))}"
        -1 -> "ONTEM - ${dayMonthFormat.format(Date(timestamp))}"
        else -> fullDateFormat.format(Date(timestamp))
    }
}

