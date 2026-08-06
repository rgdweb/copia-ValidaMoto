package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "moto")
data class Moto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val marca: String,
    val modelo: String,
    val ano: Int,
    val placa: String,
    val kmAtual: Int,
    val status: String, // "Disponível" | "Em manutenção"
    val fotoCadastro: String
)
