package com.example.core.database.dao

import androidx.room.*
import com.example.core.database.entity.Agendamento
import kotlinx.coroutines.flow.Flow

data class AgendamentoWithDetails(
    val id: Long,
    val alunoId: Long,
    val motoId: Long,
    val alunoNome: String,
    val alunoFoto: String,
    val motoModelo: String,
    val motoPlaca: String,
    val dataHora: Long,
    val status: String,
    val observacoes: String
)

@Dao
interface AgendamentoDao {
    @Query("""
        SELECT ag.id, ag.alunoId, ag.motoId, 
               al.nome as alunoNome, al.fotoCadastro as alunoFoto,
               m.modelo as motoModelo, m.placa as motoPlaca, 
               ag.dataHora, ag.status, ag.observacoes
        FROM agendamento ag 
        JOIN aluno al ON ag.alunoId = al.id 
        JOIN moto m ON ag.motoId = m.id 
        ORDER BY ag.dataHora ASC
    """)
    fun getAgendamentosWithDetailsFlow(): Flow<List<AgendamentoWithDetails>>

    @Query("SELECT * FROM agendamento WHERE id = :id")
    suspend fun getById(id: Long): Agendamento?

    @Query("SELECT COUNT(*) FROM agendamento WHERE alunoId = :alunoId")
    suspend fun getCountAgendamentosByAlunoId(alunoId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(agendamento: Agendamento): Long

    @Update
    suspend fun update(agendamento: Agendamento)

    @Delete
    suspend fun delete(agendamento: Agendamento)
}
