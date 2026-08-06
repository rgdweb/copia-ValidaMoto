package com.example.core.database.dao

import androidx.room.*
import com.example.core.database.entity.Aula
import kotlinx.coroutines.flow.Flow

data class AulaWithDetails(
    val id: Long,
    val alunoId: Long,
    val instrutorId: Long,
    val motoId: Long,
    val alunoNome: String,
    val alunoFoto: String,
    val alunoCpf: String,
    val alunoAulasContratadas: Int = 20,
    val alunoAulasRealizadas: Int = 0,
    val instrutorNome: String,
    val instrutorCnh: String,
    val instrutorValidadeCnh: String,
    val motoModelo: String,
    val motoPlaca: String,
    val dataHoraInicio: Long,
    val dataHoraFim: Long,
    val duracaoMinutos: Int,
    val kmInicial: Int,
    val kmFinal: Int,
    val kmPercorrido: Int,
    val fotoPainelInicio: String,
    val fotoPainelFim: String,
    val observacoes: String,
    val statusAula: String,
    val aulasConfirmadasAteEntao: Int,
    val uuid: String,
    val etapa: Int,
    val progressoEtapa: Int
)

@Dao
interface AulaDao {
    @Query("""
        SELECT a.id, a.alunoId, a.instrutorId, a.motoId, 
               al.nome as alunoNome, al.fotoCadastro as alunoFoto, al.cpf as alunoCpf, al.aulasContratadas as alunoAulasContratadas, al.aulasRealizadas as alunoAulasRealizadas,
               i.nome as instrutorNome, i.cnh as instrutorCnh, i.validadeCnh as instrutorValidadeCnh,
               m.modelo as motoModelo, m.placa as motoPlaca, 
               a.dataHoraInicio, a.dataHoraFim, a.duracaoMinutos, 
               a.kmInicial, a.kmFinal, a.kmPercorrido, 
               a.fotoPainelInicio, a.fotoPainelFim, a.observacoes, 
               a.statusAula, a.aulasConfirmadasAteEntao, a.uuid as uuid,
               a.etapa, a.progressoEtapa
        FROM aula a 
        JOIN aluno al ON a.alunoId = al.id 
        JOIN instrutor i ON a.instrutorId = i.id 
        JOIN moto m ON a.motoId = m.id 
        ORDER BY a.dataHoraInicio DESC
    """)
    fun getAulasWithDetailsFlow(): Flow<List<AulaWithDetails>>

    @Query("""
        SELECT a.id, a.alunoId, a.instrutorId, a.motoId, 
               al.nome as alunoNome, al.fotoCadastro as alunoFoto, al.cpf as alunoCpf, al.aulasContratadas as alunoAulasContratadas, al.aulasRealizadas as alunoAulasRealizadas,
               i.nome as instrutorNome, i.cnh as instrutorCnh, i.validadeCnh as instrutorValidadeCnh,
               m.modelo as motoModelo, m.placa as motoPlaca, 
               a.dataHoraInicio, a.dataHoraFim, a.duracaoMinutos, 
               a.kmInicial, a.kmFinal, a.kmPercorrido, 
               a.fotoPainelInicio, a.fotoPainelFim, a.observacoes, 
               a.statusAula, a.aulasConfirmadasAteEntao, a.uuid as uuid,
               a.etapa, a.progressoEtapa
        FROM aula a 
        JOIN aluno al ON a.alunoId = al.id 
        JOIN instrutor i ON a.instrutorId = i.id 
        JOIN moto m ON a.motoId = m.id 
        WHERE a.id = :id
    """)
    suspend fun getAulaWithDetailsById(id: Long): AulaWithDetails?

    @Query("SELECT * FROM aula WHERE id = :id")
    suspend fun getAulaById(id: Long): Aula?

    @Query("SELECT COUNT(*) FROM aula WHERE alunoId = :alunoId AND statusAula = 'confirmada'")
    suspend fun getCountAulasConfirmadas(alunoId: Long): Int

    @Query("SELECT COUNT(*) FROM aula WHERE alunoId = :alunoId AND statusAula = 'pendente' AND dataHoraFim = 0")
    suspend fun getCountActiveAulasByAlunoId(alunoId: Long): Int

    @Query("SELECT COUNT(*) FROM aula WHERE alunoId = :alunoId")
    suspend fun getCountAulasByAlunoId(alunoId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(aula: Aula): Long

    @Update
    suspend fun update(aula: Aula)

    @Delete
    suspend fun delete(aula: Aula)
}
