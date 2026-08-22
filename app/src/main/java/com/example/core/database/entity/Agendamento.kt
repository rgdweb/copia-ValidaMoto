package com.example.core.database.entity
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index
@Entity(tableName = "agendamento", indices = [Index(value = ["alunoId"]), Index(value = ["motoId"])], foreignKeys = [
    ForeignKey(entity = Aluno::class, parentColumns = ["id"], childColumns = ["alunoId"], onDelete = ForeignKey.NO_ACTION),
    ForeignKey(entity = Moto::class, parentColumns = ["id"], childColumns = ["motoId"], onDelete = ForeignKey.NO_ACTION)
])
data class Agendamento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alunoId: Long,
    val motoId: Long?,
    val dataHora: Long,
    val status: String = "agendada",
    val observacoes: String = "",
    val tipo: String = "AULA"
)
