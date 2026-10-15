package com.medexpiry.app.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.medexpiry.app.MainActivity
import com.medexpiry.app.MedExpiryApp
import com.medexpiry.app.R
import com.medexpiry.app.data.Medicine
import java.time.LocalDate

class ExpiryCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as MedExpiryApp).repository
        val medicines = repository.getAllOnce()
        val today = LocalDate.now()

        medicines.forEach { medicine ->
            val daysLeft = medicine.daysUntilExpiration()
            // Уведомляем за 30, 14, 7, 3, 1 день и в день истечения
            if (daysLeft in listOf(30L, 14L, 7L, 3L, 1L, 0L) || daysLeft < 0) {
                sendNotification(medicine, daysLeft)
            }
        }
        return Result.success()
    }

    private fun sendNotification(medicine: Medicine, daysLeft: Long) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            medicine.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, text) = when {
            daysLeft < 0 -> "⚠️ Срок истёк!" to "${medicine.name} — срок годности уже прошёл"
            daysLeft == 0L -> "⚠️ Сегодня истекает!" to "${medicine.name} — срок годности заканчивается сегодня"
            daysLeft == 1L -> "🔔 Завтра истекает" to "${medicine.name} — остался 1 день"
            else -> "🔔 Скоро истекает" to "${medicine.name} — осталось $daysLeft дней"
        }

        val notification = NotificationCompat.Builder(applicationContext, MedExpiryApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(medicine.id.toInt() + (daysLeft * 1000).toInt(), notification)
    }
}
