package com.example.feature.cadastros.presentation.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.BorderStroke
import coil.compose.AsyncImage
import com.example.core.database.entity.Aluno
import com.example.core.database.entity.Moto
import com.example.ui.theme.OrangeAutoescola
import com.example.ui.theme.DarkGrey
import com.example.feature.cadastros.presentation.CadastrosViewModel
import com.example.feature.cadastros.presentation.CadastrosUiEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastrosScreen(viewModel: CadastrosViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Alunos, 1 = Motos
    val state by viewModel.uiState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearToastMessage()
        }
    }
    val allAlunos = state.alunos
    val allMotos = state.motos
    val allAulas = state.aulas

    var showAddAlunoDialog by remember { mutableStateOf(false) }
    var showAddMotoDialog by remember { mutableStateOf(false) }

    // Detail Dialog States
    var detailAluno by remember { mutableStateOf<Aluno?>(null) }
    var detailMoto by remember { mutableStateOf<Moto?>(null) }

    // Edit Form Dialog States
    var showEditAlunoDialog by remember { mutableStateOf<Aluno?>(null) }
    var showEditMotoDialog by remember { mutableStateOf<Moto?>(null) }

    // Delete Confirmation States
    var showConfirmDeleteAluno by remember { mutableStateOf<Aluno?>(null) }
    var showConfirmDeleteMoto by remember { mutableStateOf<Moto?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cadastros ", fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))
                        Text("de Campo", fontWeight = FontWeight.Light, color = OrangeAutoescola)
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showAddAlunoDialog = true else showAddMotoDialog = true
                },
                containerColor = OrangeAutoescola,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add New")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = OrangeAutoescola,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = OrangeAutoescola
                    )
                },
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = Color(0xFFF5F5F5),
                        start = androidx.compose.ui.geometry.Offset(0f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Alunos", fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Alunos") },
                    selectedContentColor = OrangeAutoescola,
                    unselectedContentColor = DarkGrey.copy(alpha = 0.6f)
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Motos", fontWeight = FontWeight.Bold) },
                    icon = { Icon(imageVector = Icons.Default.TwoWheeler, contentDescription = "Motos") },
                    selectedContentColor = OrangeAutoescola,
                    unselectedContentColor = DarkGrey.copy(alpha = 0.6f)
                )
            }

            // Tabs Content
            if (selectedTab == 0) {
                // Alunos Tab
                var searchQuery by remember { mutableStateOf("") }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Modern smart search field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por Nome, CPF ou Telefone...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpar")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeAutoescola,
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                            focusedLabelColor = OrangeAutoescola
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    val filteredAlunos = remember(allAlunos, searchQuery) {
                        if (searchQuery.isBlank()) {
                            allAlunos
                        } else {
                            allAlunos.filter { aluno ->
                                aluno.nome.contains(searchQuery, ignoreCase = true) ||
                                aluno.telefone.contains(searchQuery, ignoreCase = true) ||
                                aluno.cpf.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }

                    if (filteredAlunos.isEmpty()) {
                        if (searchQuery.isNotEmpty()) {
                            EmptyListPlaceholder("Nenhum Aluno encontrado", "A busca por \"$searchQuery\" não retornou resultados.")
                        } else {
                            EmptyListPlaceholder("Nenhum Aluno cadastrado", "Use o botão + para cadastrar novos alunos.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredAlunos) { aluno ->
                                AlunoRow(aluno = aluno, onClick = { detailAluno = aluno })
                            }
                        }
                    }
                }
            } else {
                // Motos Tab
                if (allMotos.isEmpty()) {
                    EmptyListPlaceholder("Nenhuma Moto cadastrada", "Use o botão + para cadastrar novas motocicletas.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allMotos) { moto ->
                            MotoRow(moto = moto, onClick = { detailMoto = moto })
                        }
                    }
                }
            }
        }
    }

    // Aluno Details Dialog
    if (detailAluno != null) {
        val aluno = detailAluno!!
        val history = allAulas.filter { it.alunoId == aluno.id && it.statusAula == "confirmada" }

        AlertDialog(
            onDismissRequest = { detailAluno = null },
            title = { Text(text = aluno.nome, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Image in details
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                            .border(2.dp, OrangeAutoescola, CircleShape)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        if (aluno.fotoCadastro.isNotEmpty()) {
                            AsyncImage(
                                model = File(aluno.fotoCadastro),
                                contentDescription = "aluno profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "aluno", modifier = Modifier.size(48.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (aluno.cpf.isNotEmpty()) {
                        Text("CPF: ${aluno.cpf}", fontSize = 14.sp)
                    }
                    Text("Telefone: ${aluno.telefone}", fontSize = 14.sp)
                    if (aluno.dataExame.isNotBlank()) {
                        val horaExameFmt = if (aluno.horaExame.isNotBlank()) " ${aluno.horaExame}" else ""
                        Text("Exame Agendado: ${aluno.dataExame}$horaExameFmt", fontSize = 14.sp)
                    }
                    Text("Status: ${aluno.status}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Aulas: ${aluno.aulasRealizadas} realizadas / ${aluno.aulasContratadas} contratadas", fontSize = 14.sp)
                    if (aluno.observacoes.isNotEmpty()) {
                        Text("Observações: ${aluno.observacoes}", fontSize = 14.sp, color = Color.Gray)
                    }

                    Divider()

                    Text("Histórico de Aulas (${history.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    if (history.isEmpty()) {
                        Text("Nenhuma aula realizada até o momento.", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        history.forEach { classItem ->
                            val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Data: ${df.format(Date(classItem.dataHoraInicio))}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("Moto: ${classItem.motoModelo} [${classItem.motoPlaca}]", fontSize = 12.sp)
                                    Text("KM Percorrido: ${classItem.kmPercorrido} km (${classItem.kmInicial} - ${classItem.kmFinal})", fontSize = 12.sp)
                                    Text("Duração: ${classItem.duracaoMinutos} min", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showConfirmDeleteAluno = aluno },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE53935))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir")
                    }

                    Row {
                        TextButton(onClick = { showEditAlunoDialog = aluno }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Editar", color = OrangeAutoescola)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { detailAluno = null }) {
                            Text("Fechar", color = Color.Gray)
                        }
                    }
                }
            }
        )
    }

    // Moto Details Dialog
    if (detailMoto != null) {
        val moto = detailMoto!!
        val history = allAulas.filter { it.motoId == moto.id && it.statusAula == "confirmada" }

        AlertDialog(
            onDismissRequest = { detailMoto = null },
            title = { Text(text = "${moto.marca} ${moto.modelo}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Moto picture in details
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                            .border(2.dp, OrangeAutoescola, CircleShape)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        if (moto.fotoCadastro.isNotEmpty()) {
                            AsyncImage(
                                model = File(moto.fotoCadastro),
                                contentDescription = "moto profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(imageVector = Icons.Default.TwoWheeler, contentDescription = "moto", modifier = Modifier.size(48.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("Marca: ${moto.marca}", fontSize = 14.sp)
                    Text("Modelo: ${moto.modelo}", fontSize = 14.sp)
                    Text("Ano: ${moto.ano}", fontSize = 14.sp)
                    Text("Placa: ${moto.placa}", fontSize = 14.sp)
                    Text("KM Atual: ${moto.kmAtual} km", fontSize = 14.sp)
                    Text("Status: ${moto.status}", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    Divider()

                    Text("Histórico de Aulas com a Moto (${history.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    if (history.isEmpty()) {
                        Text("Nenhuma aula confirmada com esta moto.", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        history.forEach { classItem ->
                            val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Data: ${df.format(Date(classItem.dataHoraInicio))}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("Aluno: ${classItem.alunoNome}", fontSize = 12.sp)
                                    Text("KM Percorrido: ${classItem.kmPercorrido} km (${classItem.kmInicial} - ${classItem.kmFinal})", fontSize = 12.sp)
                                    Text("Duração: ${classItem.duracaoMinutos} min", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showConfirmDeleteMoto = moto },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE53935))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excluir")
                    }

                    Row {
                        TextButton(onClick = { showEditMotoDialog = moto }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Editar", color = OrangeAutoescola)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { detailMoto = null }) {
                            Text("Fechar", color = Color.Gray)
                        }
                    }
                }
            }
        )
    }

    // Add Aluno Dialog Form
    if (showAddAlunoDialog) {
        AddAlunoDialogForm(
            viewModel = viewModel,
            onDismiss = { showAddAlunoDialog = false }
        )
    }

    // Add Moto Dialog Form
    if (showAddMotoDialog) {
        AddMotoDialogForm(
            viewModel = viewModel,
            onDismiss = { showAddMotoDialog = false }
        )
    }

    // Edit Aluno Dialog Form
    if (showEditAlunoDialog != null) {
        EditAlunoDialogForm(
            aluno = showEditAlunoDialog!!,
            viewModel = viewModel,
            onDismiss = {
                showEditAlunoDialog = null
                detailAluno = null // refresh details
            }
        )
    }

    // Edit Moto Dialog Form
    if (showEditMotoDialog != null) {
        EditMotoDialogForm(
            moto = showEditMotoDialog!!,
            viewModel = viewModel,
            onDismiss = {
                showEditMotoDialog = null
                detailMoto = null // refresh details
            }
        )
    }

    // Delete Confirmations
    if (showConfirmDeleteAluno != null) {
        val al = showConfirmDeleteAluno!!
        AlertDialog(
            onDismissRequest = { showConfirmDeleteAluno = null },
            title = { Text("Excluir Aluno", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja mesmo excluir o cadastro do aluno ${al.nome}? Esta ação é permanente e apagará todos os seus registros.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(CadastrosUiEvent.DeleteStudent(al))
                        showConfirmDeleteAluno = null
                        detailAluno = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteAluno = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    if (showConfirmDeleteMoto != null) {
        val mo = showConfirmDeleteMoto!!
        AlertDialog(
            onDismissRequest = { showConfirmDeleteMoto = null },
            title = { Text("Excluir Moto", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja mesmo excluir a moto ${mo.marca} ${mo.modelo} [${mo.placa}]? Esta ação é permanente e apagará todos os seus registros.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(CadastrosUiEvent.DeleteMoto(mo))
                        showConfirmDeleteMoto = null
                        detailMoto = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteMoto = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun AlunoRow(aluno: Aluno, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            // Profile image
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEEEEE))
                    .border(2.dp, OrangeAutoescola, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (aluno.fotoCadastro.isNotEmpty()) {
                    AsyncImage(
                        model = File(aluno.fotoCadastro),
                        contentDescription = "student photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "student", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = aluno.nome, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1C1B1F))
                if (aluno.cpf.isNotEmpty()) {
                    Text(text = "CPF: ${aluno.cpf}", fontSize = 12.sp, color = Color.Gray)
                }
                Text(text = "Tel: ${aluno.telefone}", fontSize = 12.sp, color = Color.Gray)
                Text(text = "Aulas: ${aluno.aulasRealizadas} de ${aluno.aulasContratadas}", fontSize = 12.sp, color = Color.Gray)
            }

            // Status chip
            val isCompleted = aluno.status == "Concluído"
            SuggestionChip(
                onClick = {},
                label = { Text(aluno.status, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    labelColor = if (isCompleted) Color(0xFF4CAF50) else OrangeAutoescola,
                    containerColor = if (isCompleted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                )
            )
        }
    }
}

@Composable
fun MotoRow(moto: Moto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEEEEE))
                    .border(2.dp, OrangeAutoescola, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (moto.fotoCadastro.isNotEmpty()) {
                    AsyncImage(
                        model = File(moto.fotoCadastro),
                        contentDescription = "moto photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(imageVector = Icons.Default.TwoWheeler, contentDescription = "moto", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${moto.marca} ${moto.modelo}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1C1B1F))
                Text(text = "Placa: ${moto.placa}  |  Ano: ${moto.ano}", fontSize = 12.sp, color = Color.Gray)
                Text(text = "KM atual: ${moto.kmAtual} km", fontSize = 12.sp, color = Color.Gray)
            }

            // Status chip
            val isAvail = moto.status == "Disponível"
            SuggestionChip(
                onClick = {},
                label = { Text(moto.status, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    labelColor = if (isAvail) Color(0xFF4CAF50) else Color(0xFFE53935),
                    containerColor = if (isAvail) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            )
        }
    }
}

@Composable
fun EmptyListPlaceholder(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = "Empty",
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = title, fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlunoDialogForm(viewModel: CadastrosViewModel, onDismiss: () -> Unit) {
    var nome by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var contratadas by remember { mutableStateOf("") }
    var examenDate by remember { mutableStateOf("") }
    var examenTime by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Picture taking
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoFile?.let {
                photoPath = it.absolutePath
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = viewModel.createPhotoFile(context, "cad_aluno")
            if (copyUriToFile(context, uri, file)) {
                photoPath = file.absolutePath
            } else {
                Toast.makeText(context, "Falha ao carregar imagem da galeria", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = { Text("Selecionar Foto", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showPhotoSourceDialog = false
                            val file = viewModel.createPhotoFile(context, "cad_aluno")
                            tempPhotoFile = file
                            tempUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cadastrar Aluno", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Photo Slot
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEEEE))
                        .border(1.dp, OrangeAutoescola, CircleShape)
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            showPhotoSourceDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoPath.isNotEmpty()) {
                        AsyncImage(
                            model = File(photoPath),
                            contentDescription = "captured profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Cam", tint = Color.Gray)
                    }
                }

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome Completo") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cpf,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }.take(11)
                        cpf = clean
                    },
                    label = { Text("CPF") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: 123.456.789-00") },
                    visualTransformation = CpfVisualTransformation()
                )

                OutlinedTextField(
                    value = telefone,
                    onValueChange = { telefone = it },
                    label = { Text("Telefone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contratadas,
                    onValueChange = { contratadas = it },
                    label = { Text("Aulas Contratadas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = examenDate,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }.take(8)
                        examenDate = clean
                    },
                    label = { Text("Data do Exame (DD/MM/AAAA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: 15/09/2026") },
                    visualTransformation = DateVisualTransformation()
                )

                OutlinedTextField(
                    value = examenTime,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }.take(4)
                        examenTime = clean
                    },
                    label = { Text("Hora do Exame (HH:mm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: 14:30") },
                    visualTransformation = TimeVisualTransformation()
                )

                OutlinedTextField(
                    value = observacoes,
                    onValueChange = { observacoes = it },
                    label = { Text("Observações") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val numContratadas = contratadas.toIntOrNull()
                    if (nome.isEmpty()) {
                        Toast.makeText(context, "O nome do aluno é obrigatório", Toast.LENGTH_SHORT).show()
                    } else if (numContratadas == null || numContratadas <= 0) {
                        Toast.makeText(context, "Aulas contratadas deve ser maior que zero", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.onEvent(
                            CadastrosUiEvent.AddStudent(
                                nome = nome,
                                cpf = formatCpf(cpf),
                                telefone = telefone,
                                contratadas = numContratadas,
                                realizadas = 0,
                                status = "Em andamento",
                                exame = formatDate(examenDate),
                                horaExame = formatTime(examenTime),
                                obs = observacoes,
                                foto = photoPath
                            )
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMotoDialogForm(viewModel: CadastrosViewModel, onDismiss: () -> Unit) {
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var ano by remember { mutableStateOf("") }
    var placa by remember { mutableStateOf("") }
    var kmStr by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Disponível") }
    var photoPath by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Camera picture
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoFile?.let {
                photoPath = it.absolutePath
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cadastrar Moto", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEEEE))
                        .border(1.dp, OrangeAutoescola, CircleShape)
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            val file = viewModel.createPhotoFile(context, "cad_moto")
                            tempPhotoFile = file
                            tempUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            cameraLauncher.launch(tempUri!!)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoPath.isNotEmpty()) {
                        AsyncImage(
                            model = File(photoPath),
                            contentDescription = "moto custom image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Cam", tint = Color.Gray)
                    }
                }

                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it },
                    label = { Text("Marca") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: Honda, Yamaha") }
                )

                OutlinedTextField(
                    value = modelo,
                    onValueChange = { modelo = it },
                    label = { Text("Modelo") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ano,
                    onValueChange = { ano = it },
                    label = { Text("Ano") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = placa,
                    onValueChange = { placa = it },
                    label = { Text("Placa") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: BRA2E19") }
                )

                OutlinedTextField(
                    value = kmStr,
                    onValueChange = { kmStr = it },
                    label = { Text("Quilometragem Atual") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Dropdown
                var expandedStatus by remember { mutableStateOf(false) }
                Column {
                    Text("Status da Moto:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .clickable { expandedStatus = true }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(status)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "drop")
                        }
                    }
                    DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                        DropdownMenuItem(text = { Text("Disponível") }, onClick = { status = "Disponível"; expandedStatus = false })
                        DropdownMenuItem(text = { Text("Em manutenção") }, onClick = { status = "Em manutenção"; expandedStatus = false })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (marca.isNotEmpty() && modelo.isNotEmpty() && placa.isNotEmpty()) {
                        viewModel.onEvent(
                            CadastrosUiEvent.AddMoto(
                                marca = marca,
                                modelo = modelo,
                                ano = ano.toIntOrNull() ?: 2024,
                                placa = placa.uppercase(),
                                km = kmStr.toIntOrNull() ?: 0,
                                status = status,
                                foto = photoPath
                            )
                        )
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Preencha marca, modelo e placa", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlunoDialogForm(aluno: Aluno, viewModel: CadastrosViewModel, onDismiss: () -> Unit) {
    var nome by remember { mutableStateOf(aluno.nome) }
    var cpf by remember { mutableStateOf(aluno.cpf.filter { it.isDigit() }) }
    var telefone by remember { mutableStateOf(aluno.telefone) }
    var contratadas by remember { mutableStateOf(aluno.aulasContratadas.toString()) }
    var realizadas by remember { mutableStateOf(aluno.aulasRealizadas.toString()) }
    var status by remember { mutableStateOf(aluno.status) }
    var examenDate by remember { mutableStateOf(aluno.dataExame.filter { it.isDigit() }) }
    var examenTime by remember { mutableStateOf(aluno.horaExame.filter { it.isDigit() }) }
    var observacoes by remember { mutableStateOf(aluno.observacoes) }
    var photoPath by remember { mutableStateOf(aluno.fotoCadastro) }

    val context = LocalContext.current

    // Picture taking
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoFile?.let {
                photoPath = it.absolutePath
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = viewModel.createPhotoFile(context, "cad_aluno_edit")
            if (copyUriToFile(context, uri, file)) {
                photoPath = file.absolutePath
            } else {
                Toast.makeText(context, "Falha ao carregar imagem da galeria", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = { Text("Selecionar Foto", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showPhotoSourceDialog = false
                            val file = viewModel.createPhotoFile(context, "cad_aluno_edit")
                            tempPhotoFile = file
                            tempUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Aluno", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Photo Slot
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEEEE))
                        .border(1.dp, OrangeAutoescola, CircleShape)
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            showPhotoSourceDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoPath.isNotEmpty()) {
                        AsyncImage(
                            model = File(photoPath),
                            contentDescription = "captured profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Cam", tint = Color.Gray)
                    }
                }

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome Completo") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cpf,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }.take(11)
                        cpf = clean
                    },
                    label = { Text("CPF") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: 123.456.789-00") },
                    visualTransformation = CpfVisualTransformation()
                )

                OutlinedTextField(
                    value = telefone,
                    onValueChange = { telefone = it },
                    label = { Text("Telefone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contratadas,
                    onValueChange = { contratadas = it },
                    label = { Text("Aulas Contratadas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = realizadas,
                    onValueChange = { realizadas = it },
                    label = { Text("Aulas Realizadas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Status selection
                var expandedStatus by remember { mutableStateOf(false) }
                Column {
                    Text("Status do Aluno:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .clickable { expandedStatus = true }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(status)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "drop")
                        }
                    }
                    DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                        DropdownMenuItem(text = { Text("Em andamento") }, onClick = { status = "Em andamento"; expandedStatus = false })
                        DropdownMenuItem(text = { Text("Concluído") }, onClick = { status = "Concluído"; expandedStatus = false })
                    }
                }

                OutlinedTextField(
                    value = examenDate,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }.take(8)
                        examenDate = clean
                    },
                    label = { Text("Data do Exame (DD/MM/AAAA)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: 15/09/2026") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = DateVisualTransformation()
                )

                OutlinedTextField(
                    value = examenTime,
                    onValueChange = { input ->
                        val clean = input.filter { it.isDigit() }.take(4)
                        examenTime = clean
                    },
                    label = { Text("Hora do Exame (HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: 14:30") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = TimeVisualTransformation()
                )

                OutlinedTextField(
                    value = observacoes,
                    onValueChange = { observacoes = it },
                    label = { Text("Observações") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nome.isNotEmpty() && telefone.isNotEmpty()) {
                        val updated = aluno.copy(
                            nome = nome,
                            cpf = formatCpf(cpf),
                            telefone = telefone,
                            aulasContratadas = contratadas.toIntOrNull() ?: aluno.aulasContratadas,
                            aulasRealizadas = realizadas.toIntOrNull() ?: aluno.aulasRealizadas,
                            status = status,
                            dataExame = formatDate(examenDate),
                            horaExame = formatTime(examenTime),
                            observacoes = observacoes,
                            fotoCadastro = photoPath
                        )
                        viewModel.onEvent(CadastrosUiEvent.UpdateStudent(updated))
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Nome e telefone são obrigatórios", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMotoDialogForm(moto: Moto, viewModel: CadastrosViewModel, onDismiss: () -> Unit) {
    var marca by remember { mutableStateOf(moto.marca) }
    var modelo by remember { mutableStateOf(moto.modelo) }
    var ano by remember { mutableStateOf(moto.ano.toString()) }
    var placa by remember { mutableStateOf(moto.placa) }
    var kmStr by remember { mutableStateOf(moto.kmAtual.toString()) }
    var status by remember { mutableStateOf(moto.status) }
    var photoPath by remember { mutableStateOf(moto.fotoCadastro) }

    val context = LocalContext.current

    // Camera picture
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoFile?.let {
                photoPath = it.absolutePath
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Moto", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEEEEE))
                        .border(1.dp, OrangeAutoescola, CircleShape)
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            val file = viewModel.createPhotoFile(context, "cad_moto_edit")
                            tempPhotoFile = file
                            tempUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            cameraLauncher.launch(tempUri!!)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (photoPath.isNotEmpty()) {
                        AsyncImage(
                            model = File(photoPath),
                            contentDescription = "moto custom image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Cam", tint = Color.Gray)
                    }
                }

                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it },
                    label = { Text("Marca") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = modelo,
                    onValueChange = { modelo = it },
                    label = { Text("Modelo") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ano,
                    onValueChange = { ano = it },
                    label = { Text("Ano") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = placa,
                    onValueChange = { placa = it },
                    label = { Text("Placa") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = kmStr,
                    onValueChange = { kmStr = it },
                    label = { Text("Quilometragem Atual") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Dropdown
                var expandedStatus by remember { mutableStateOf(false) }
                Column {
                    Text("Status da Moto:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .clickable { expandedStatus = true }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(status)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "drop")
                        }
                    }
                    DropdownMenu(expanded = expandedStatus, onDismissRequest = { expandedStatus = false }) {
                        DropdownMenuItem(text = { Text("Disponível") }, onClick = { status = "Disponível"; expandedStatus = false })
                        DropdownMenuItem(text = { Text("Em manutenção") }, onClick = { status = "Em manutenção"; expandedStatus = false })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (marca.isNotEmpty() && modelo.isNotEmpty() && placa.isNotEmpty()) {
                        val updated = moto.copy(
                            marca = marca,
                            modelo = modelo,
                            ano = ano.toIntOrNull() ?: moto.ano,
                            placa = placa.uppercase(),
                            kmAtual = kmStr.toIntOrNull() ?: moto.kmAtual,
                            status = status,
                            fotoCadastro = photoPath
                        )
                        viewModel.onEvent(CadastrosUiEvent.UpdateMoto(updated))
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Preencha marca, modelo e placa", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAutoescola)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

private fun copyUriToFile(context: android.content.Context, uri: Uri, destFile: File): Boolean {
    return com.example.util.FileHelper.copyUriToFile(context, uri, destFile)
}

fun formatCpf(input: String): String {
    val clean = input.filter { it.isDigit() }.take(11)
    val sb = StringBuilder()
    for (i in clean.indices) {
        if (i == 3 || i == 6) {
            sb.append('.')
        } else if (i == 9) {
            sb.append('-')
        }
        sb.append(clean[i])
    }
    return sb.toString()
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

private fun formatTime(input: String): String {
    val clean = input.filter { it.isDigit() }.take(4)
    if (clean.length < 4) return ""
    val sb = StringBuilder()
    for (i in clean.indices) {
        if (i == 2) {
            sb.append(':')
        }
        sb.append(clean[i])
    }
    return sb.toString()
}

class TimeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(4)
        val formatted = StringBuilder()
        for (i in digits.indices) {
            if (i == 2) formatted.append(':')
            formatted.append(digits[i])
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val mapped = when {
                    offset <= 2 -> offset
                    else -> offset + 1
                }
                return mapped.coerceIn(0, formatted.length)
            }
            override fun transformedToOriginal(offset: Int): Int {
                val mapped = when {
                    offset <= 2 -> offset
                    offset <= 3 -> 2
                    else -> offset - 1
                }
                return mapped.coerceIn(0, digits.length)
            }
        }
        return TransformedText(AnnotatedString(formatted.toString()), offsetMapping)
    }
}

class CpfVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val formatted = StringBuilder()
        for (i in originalText.indices) {
            if (i == 3 || i == 6) {
                formatted.append('.')
            } else if (i == 9) {
                formatted.append('-')
            }
            formatted.append(originalText[i])
        }
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val mapped = when {
                    offset <= 3 -> offset
                    offset <= 6 -> offset + 1
                    offset <= 9 -> offset + 2
                    else -> offset + 3
                }
                return mapped.coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val mapped = when {
                    offset <= 3 -> offset
                    offset <= 7 -> offset - 1
                    offset <= 11 -> offset - 2
                    else -> offset - 3
                }
                return mapped.coerceIn(0, originalText.length)
            }
        }
        
        return TransformedText(AnnotatedString(formatted.toString()), offsetMapping)
    }
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
