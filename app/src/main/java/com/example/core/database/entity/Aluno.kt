package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "aluno",
    indices = [Index(value = ["cpf"], unique = false)]
)
data class Aluno(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val cpf: String = "",
    val telefone: String,
    val aulasContratadas: Int,
    val aulasRealizadas: Int,
    val status: String, // "Em andamento" | "Concluído" | "Inativo"
    val dataExame: String,
    val observacoes: String,
    val fotoCadastro: String
)
