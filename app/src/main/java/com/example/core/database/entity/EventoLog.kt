package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "evento_log")
data class EventoLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val tipo: String,  // "aula_criada", "aula_iniciada", "tempo_aumentado", "checkout_iniciado", "foto_capturada", "km_alterado", "pdf_gerado", "backup_realizado", "sincronizacao", "aula_confirmada", "sessao_encerrada", "aula_cancelada"
    val usuario: String,
    val alunoId: Long?,
    val alunoNome: String?,
    val instrutorId: Long?,
    val instrutorNome: String?,
    val motoId: Long?,
    val motoModelo: String?,
    val descricao: String
)
