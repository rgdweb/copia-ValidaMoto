package com.example.feature.cadastros.data.repository

import com.example.core.database.AppDatabase
import com.example.core.database.entity.Aluno
import com.example.core.database.entity.Moto
import com.example.core.database.dao.AulaWithDetails
import com.example.feature.cadastros.domain.repository.CadastrosRepository
import kotlinx.coroutines.flow.Flow

class CadastrosRepositoryImpl(private val db: AppDatabase) : CadastrosRepository {
    private val alunoDao = db.alunoDao()
    private val motoDao = db.motoDao()
    private val aulaDao = db.aulaDao()
    private val agendamentoDao = db.agendamentoDao()

    override fun getAlunosFlow(): Flow<List<Aluno>> = alunoDao.getAlunosFlow()
    override fun getMotosFlow(): Flow<List<Moto>> = motoDao.getMotosFlow()
    override fun getAulasWithDetailsFlow(): Flow<List<AulaWithDetails>> = aulaDao.getAulasWithDetailsFlow()

    override suspend fun insertAluno(aluno: Aluno): Long = alunoDao.insert(aluno)
    override suspend fun updateAluno(aluno: Aluno) = alunoDao.update(aluno)
    override suspend fun deleteAluno(aluno: Aluno) = alunoDao.delete(aluno)
    override suspend fun countActiveAulasByAluno(alunoId: Long): Int = aulaDao.getCountActiveAulasByAlunoId(alunoId)
    override suspend fun countAulasByAluno(alunoId: Long): Int = aulaDao.getCountAulasByAlunoId(alunoId)
    override suspend fun countAgendamentosByAluno(alunoId: Long): Int = agendamentoDao.getCountAgendamentosByAlunoId(alunoId)

    override suspend fun insertMoto(moto: Moto): Long = motoDao.insert(moto)
    override suspend fun updateMoto(moto: Moto) = motoDao.update(moto)
    override suspend fun deleteMoto(moto: Moto) = motoDao.delete(moto)
}
