package com.example.feature.aula.presentation.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.util.SoundAndVibrationHelper

class AulaAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val title = intent.getStringExtra("title") ?: "Aviso de Aula"
        val message = intent.getStringExtra("message") ?: ""
        val aulaId = intent.getLongExtra("aulaId", -1L)
        val alertType = intent.getStringExtra("alertType") ?: ""
        val soundDurationMs = intent.getIntExtra("soundDurationMs", 300)
        val vibeDurationMs = intent.getIntExtra("vibeDurationMs", 200)

        Log.d("AulaAlarmReceiver", "Alarm triggered: $alertType for aula $aulaId")

        // 1. Check if alert was already triggered (e.g., by foreground ticker)
        val sharedPrefs = context.getSharedPreferences("valida_moto_prefs", Context.MODE_PRIVATE)
        if (sharedPrefs.getBoolean("alert_${aulaId}_$alertType", false)) {
            Log.d("AulaAlarmReceiver", "Alarm for $alertType on aula $aulaId already triggered, ignoring.")
            return
        }

        // Mark alert as triggered in SharedPreferences
        sharedPrefs.edit().putBoolean("alert_${aulaId}_$alertType", true).commit()

        // Use goAsync to keep BroadcastReceiver alive during background alert execution
        val pendingResult = goAsync()

        Thread {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ValidaMoto:AulaAlarmWakeLock"
            )
            try {
                wakeLock?.acquire(5000L) // Holds CPU awake for 5s while alert thread completes (~2.5s)
                val soundVib = SoundAndVibrationHelper(context)
                soundVib.triggerEmergencyAlert(synchronous = true)

                // Show System Notification
                val CHANNEL_ID = "aula_notification_channel"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val name = "Notificações de Aulas"
                    val importance = NotificationManager.IMPORTANCE_HIGH
                    val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                        description = "Notificações de alertas de tempo das aulas de direção"
                        enableLights(true)
                        enableVibration(true)
                    }
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.createNotificationChannel(channel)
                }

                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    aulaId.toInt() + alertType.hashCode(),
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify((aulaId.toInt() + alertType.hashCode()), builder.build())
            } catch (e: Exception) {
                Log.e("AulaAlarmReceiver", "Failed to trigger emergency alert or notification on alarm", e)
            } finally {
                if (wakeLock?.isHeld == true) {
                    try { wakeLock.release() } catch (e: Exception) {}
                }
                pendingResult.finish()
            }
        }.start()
    }
}
