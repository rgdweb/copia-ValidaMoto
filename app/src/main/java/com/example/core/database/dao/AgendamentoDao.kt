package com.example.core.database.dao
import androidx.room.*
import com.example.core.database.entity.Agendamento
import kotlinx.coroutines.flow.Flow

data class AgendamentoWithDetails(
    val id: Long, val alunoId: Long, val motoId: Long?,
    val alunoNome: String, val alunoFoto: String,
    val motoModelo: String?, val motoPlaca: String?,
    val dataHora: Long, val status: String, val observacoes: String, val tipo: String
)
@Dao
interface AgendamentoDao {
    @Query("""SELECT ag.id, ag.alunoId, ag.motoId, al.nome as alunoNome, al.fotoCadastro as alunoFoto, m.modelo as motoModelo, m.placa as motoPlaca, ag.dataHora, ag.status, ag.observacoes, ag.tipo FROM agendamento ag JOIN aluno al ON ag.alunoId = al.id LEFT JOIN moto m ON ag.motoId = m.id ORDER BY ag.dataHora ASC""")
    fun getAgendamentosWithDetailsFlow(): Flow<List<AgendamentoWithDetails>>
    @Query("SELECT * FROM agendamento WHERE id = :id")
    suspend fun getById(id: Long): Agendamento?
    @Query("SELECT COUNT(*) FROM agendamento WHERE alunoId = :alunoId")
    suspend fun getCountAgendamentosByAlunoId(alunoId: Long): Int
    @Query("SELECT * FROM agendamento WHERE alunoId = :alunoId AND tipo = 'EXAME' LIMIT 1")
    suspend fun getExameAgendamentoByAlunoId(alunoId: Long): Agendamento?
    @Query("SELECT * FROM agendamento ORDER BY dataHora ASC")
    suspend fun getAllAgendamentosForSync(): List<Agendamento>
    @Query("SELECT * FROM agendamento WHERE tipo = 'EXAME' AND dataHora = 0")
    suspend fun getExamesWithPlaceholderTimestamp(): List<Agendamento>
    @Query("UPDATE agendamento SET dataHora = :dataHora WHERE id = :id")
    suspend fun updateDataHora(id: Long, dataHora: Long)
    @Query("UPDATE agendamento SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(agendamento: Agendamento): Long
    @Update
    suspend fun update(agendamento: Agendamento)
    @Delete
    suspend fun delete(agendamento: Agendamento)
    @Query("DELETE FROM agendamento WHERE id = :id")
    suspend fun deleteById(id: Long)
}
