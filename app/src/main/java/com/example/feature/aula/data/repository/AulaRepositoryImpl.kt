package com.example.feature.aula.data.repository

import com.example.core.database.AppDatabase
import com.example.core.database.entity.*
import com.example.core.database.dao.AulaWithDetails
import com.example.feature.aula.domain.repository.AulaRepository
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class AulaRepositoryImpl(private val db: AppDatabase) : AulaRepository {
    private val instrutorDao = db.instrutorDao()
    private val alunoDao = db.alunoDao()
    private val motoDao = db.motoDao()
    private val aulaDao = db.aulaDao()
    private val aulaFotoDao = db.aulaFotoDao()
    private val eventoLogDao = db.eventoLogDao()

    override fun getAulasWithDetailsFlow(): Flow<List<AulaWithDetails>> = aulaDao.getAulasWithDetailsFlow()
    override fun getAlunosFlow(): Flow<List<Aluno>> = alunoDao.getAlunosFlow()
    override fun getMotosFlow(): Flow<List<Moto>> = motoDao.getMotosFlow()
    override fun getInstrutorFlow(): Flow<Instrutor?> = instrutorDao.getInstrutorFlow()
    override fun getAllLogsFlow(): Flow<List<EventoLog>> = eventoLogDao.getAllLogsFlow()

    override suspend fun getInstrutor(): Instrutor? = instrutorDao.getInstrutor()
    override suspend fun getAlunoById(id: Long): Aluno? = alunoDao.getAlunoById(id)
    override suspend fun getMotoById(id: Long): Moto? = motoDao.getMotoById(id)
    override suspend fun getAulaWithDetailsById(id: Long): AulaWithDetails? = aulaDao.getAulaWithDetailsById(id)
    override suspend fun getAulaById(id: Long): Aula? = aulaDao.getAulaById(id)
    override suspend fun getCountAulasConfirmadas(alunoId: Long): Int = aulaDao.getCountAulasConfirmadas(alunoId)
    override suspend fun getFotosForAula(aulaId: Long): List<AulaFoto> = aulaFotoDao.getFotosForAula(aulaId)

    override suspend fun insertAula(aula: Aula): Long = aulaDao.insert(aula)
    override suspend fun updateAula(aula: Aula) = aulaDao.update(aula)
    override suspend fun deleteAula(aula: Aula) = aulaDao.delete(aula)
    override suspend fun updateAluno(aluno: Aluno) = alunoDao.update(aluno)
    override suspend fun updateMoto(moto: Moto) = motoDao.update(moto)
    override suspend fun insertAulaFoto(aulaFoto: AulaFoto): Long = aulaFotoDao.insert(aulaFoto)
    override suspend fun insertAllAulaFotos(aulaFotos: List<AulaFoto>) = aulaFotoDao.insertAll(aulaFotos)
    override suspend fun insertEventoLog(log: EventoLog): Long = eventoLogDao.insert(log)
    override suspend fun saveInstrutor(instrutor: Instrutor): Long {
        val existing = instrutorDao.getInstrutor()
        return if (existing != null) {
            val updated = instrutor.copy(id = existing.id)
            instrutorDao.update(updated)
            existing.id
        } else {
            instrutorDao.insert(instrutor)
        }
    }

    override suspend fun <R> runInTransaction(block: suspend () -> R): R {
        return db.withTransaction(block)
    }
}
