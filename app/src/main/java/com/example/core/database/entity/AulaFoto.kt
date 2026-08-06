package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "aula_foto",
    indices = [
        Index(value = ["aulaId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Aula::class,
            parentColumns = ["id"],
            childColumns = ["aulaId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class AulaFoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val aulaId: Long,
    val tipo: String, // "instrutor_inicio" | "aluno_inicio" | "instrutor_fim" | "aluno_fim"
    val pose: String, // "direita" | "abaixar" | "fechar_olhos" | "sorrir"
    val caminhoFoto: String,
    val timestamp: Long
)
