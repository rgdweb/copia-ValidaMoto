package com.example.core.notifications

import android.content.Context
import android.util.Log
import com.example.core.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

class SyncAlarmsOnStartup(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun execute() {
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val agDao = db.agendamentoDao()
                val alDao = db.alunoDao()
                val sa = ScheduleAlarmUseCase(context)
                for (ex in agDao.getExamesWithPlaceholderTimestamp()) {
                    val al = alDao.getAlunoById(ex.alunoId) ?: continue
                    val dh = parseExame(al.dataExame, al.horaExame)
                    if (dh != null) agDao.updateDataHora(ex.id, dh)
                    else agDao.updateStatus(ex.id, "cancelada")
                }
                for (ag in agDao.getAllAgendamentosForSync()) {
                    if (ag.status == "agendada" && ag.dataHora > System.currentTimeMillis()) {
                        val al = alDao.getAlunoById(ag.alunoId)
                        sa.schedule(ag.id, al?.nome ?: "Aluno", ag.dataHora, ag.tipo)
                    } else if (ag.status != "agendada") {
                        sa.cancel(ag.id)
                    }
                }
            } catch (e: Exception) { Log.e("SyncAlarmsOnStartup", "Failed", e) }
        }
    }
    private fun parseExame(dataExame: String, horaExame: String): Long? {
        val p = dataExame.trim().split("/")
        if (p.size != 3) return null
        val d = p[0].toIntOrNull() ?: return null
        val m = p[1].toIntOrNull() ?: return null
        val y = p[2].toIntOrNull() ?: return null
        if (d !in 1..31 || m !in 1..12 || y < 2000) return null
        var h = 8; var mi = 0
        val ch = horaExame.trim()
        if (ch.isNotEmpty()) {
            val hp = ch.split(":")
            if (hp.size == 2) {
                val hh = hp[0].toIntOrNull(); val mm = hp[1].toIntOrNull()
                if (hh != null && mm != null && hh in 0..23 && mm in 0..59) { h = hh; mi = mm }
            }
        }
        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, y); c.set(Calendar.MONTH, m - 1); c.set(Calendar.DAY_OF_MONTH, d)
        c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, mi)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}
