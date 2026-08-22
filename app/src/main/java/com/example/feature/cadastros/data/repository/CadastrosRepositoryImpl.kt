package com.example.feature.cadastros.data.repository
import com.example.core.database.AppDatabase
import com.example.core.database.entity.Agendamento
import com.example.core.database.entity.Aluno
import com.example.core.database.entity.Moto
import com.example.core.database.dao.AulaWithDetails
import com.example.feature.cadastros.domain.repository.CadastrosRepository
import kotlinx.coroutines.flow.Flow
class CadastrosRepositoryImpl(private val db: AppDatabase) : CadastrosRepository {
    private val ad = db.alunoDao(); private val md = db.motoDao(); private val aud = db.aulaDao(); private val agd = db.agendamentoDao()
    override fun getAlunosFlow(): Flow<List<Aluno>> = ad.getAlunosFlow()
    override fun getMotosFlow(): Flow<List<Moto>> = md.getMotosFlow()
    override fun getAulasWithDetailsFlow(): Flow<List<AulaWithDetails>> = aud.getAulasWithDetailsFlow()
    override suspend fun insertAluno(a: Aluno): Long = ad.insert(a)
    override suspend fun updateAluno(a: Aluno) = ad.update(a)
    override suspend fun deleteAluno(a: Aluno) = ad.delete(a)
    override suspend fun countActiveAulasByAluno(id: Long): Int = aud.getCountActiveAulasByAlunoId(id)
    override suspend fun countAulasByAluno(id: Long): Int = aud.getCountAulasByAlunoId(id)
    override suspend fun countAgendamentosByAluno(id: Long): Int = agd.getCountAgendamentosByAlunoId(id)
    override suspend fun insertMoto(m: Moto): Long = md.insert(m)
    override suspend fun updateMoto(m: Moto) = md.update(m)
    override suspend fun deleteMoto(m: Moto) = md.delete(m)
    override suspend fun getExameAgendamentoByAlunoId(id: Long): Agendamento? = agd.getExameAgendamentoByAlunoId(id)
    override suspend fun deleteAgendamentoById(id: Long) { agd.deleteById(id) }
}
