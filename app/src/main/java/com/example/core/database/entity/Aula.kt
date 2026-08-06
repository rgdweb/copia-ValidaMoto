package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "aula",
    indices = [
        Index(value = ["alunoId"]),
        Index(value = ["instrutorId"]),
        Index(value = ["motoId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Aluno::class,
            parentColumns = ["id"],
            childColumns = ["alunoId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = Instrutor::class,
            parentColumns = ["id"],
            childColumns = ["instrutorId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = Moto::class,
            parentColumns = ["id"],
            childColumns = ["motoId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class Aula(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alunoId: Long,
    val instrutorId: Long,
    val motoId: Long,
    val dataHoraInicio: Long,
    val dataHoraFim: Long,
    val duracaoMinutos: Int,
    val kmInicial: Int,
    val kmFinal: Int,
    val kmPercorrido: Int,
    val fotoPainelInicio: String,
    val fotoPainelFim: String,
    val observacoes: String,
    val statusAula: String, // "confirmada" | "pendente" | "cancelada"
    val aulasConfirmadasAteEntao: Int,
    val uuid: String = java.util.UUID.randomUUID().toString(),
    val etapa: Int = 4,
    val progressoEtapa: Int = 0
)
