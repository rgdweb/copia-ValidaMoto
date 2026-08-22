package com.example.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.feature.aula.presentation.receiver.AulaAlarmReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleAlarmUseCase(private val context: Context) {
    companion object {
        private const val REQUEST_CODE_OFFSET = 5_000_000L
        private const val TWENTY_FOUR_HOURS_MILLIS = 24L * 60 * 60 * 1000L
    }

    fun schedule(agendamentoId: Long, alunoNome: String, dataHora: Long, tipo: String = "AULA") {
        cancel(agendamentoId)
        val triggerTime = dataHora - TWENTY_FOUR_HOURS_MILLIS
        if (triggerTime <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val horaStr = sdf.format(Date(dataHora))
        val (title, message, alertType) = when (tipo) {
            "EXAME" -> Triple("Aviso de Exame", "Amanha e o exame do(a) aluno(a) $alunoNome as $horaStr.", "exame_reminder")
            else -> Triple("Aviso de Aula", "Amanha voce possui aula com $alunoNome as $horaStr.", "agenda_reminder")
        }
        val intent = Intent(context, AulaAlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("aulaId", agendamentoId + REQUEST_CODE_OFFSET)
            putExtra("alertType", alertType)
            putExtra("soundDurationMs", 500)
            putExtra("vibeDurationMs", 300)
        }
        val pi = PendingIntent.getBroadcast(
            context, (agendamentoId + REQUEST_CODE_OFFSET).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms())
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
                else alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            }
        } catch (e: Exception) { Log.e("ScheduleAlarmUseCase", "Failed", e) }
    }

    fun cancel(agendamentoId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = PendingIntent.getBroadcast(
            context, (agendamentoId + REQUEST_CODE_OFFSET).toInt(),
            Intent(context, AulaAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) { try { am.cancel(pi); pi.cancel() } catch (e: Exception) {} }
    }
}
