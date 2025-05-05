package com.example.medireminder.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.medireminder.NotificationActionReceiver
import com.example.medireminder.R
import com.example.medireminder.data.Medication
import com.example.medireminder.data.MedicationDatabase

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "medication_reminder_channel"
        const val NOTIFICATION_ID = 1
    }

    override suspend fun doWork(): Result {
        val medicationId = inputData.getInt("medicationId", -1)
        if (medicationId == -1) return Result.failure()

        val dao = MedicationDatabase.getDatabase(applicationContext).medicationDao()
        val medication = dao.getMedicationById(medicationId) ?: return Result.failure()

        showNotification(medication)
        return Result.success()
    }

    private fun showNotification(medication: Medication) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val markTakenIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = "ACTION_MARK_TAKEN"
            putExtra("medicationId", medication.id)
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            applicationContext, medication.id, markTakenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val postponeIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = "ACTION_POSTPONE"
            putExtra("medicationId", medication.id)
        }
        val postponePendingIntent = PendingIntent.getBroadcast(
            applicationContext, medication.id + 1000, postponeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_access_time)
            .setContentTitle("Medication Reminder: ${medication.name}")
            .setContentText("Time to take ${medication.dosage} at ${medication.timeToTake}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .addAction(R.drawable.ic_check, "Mark as Taken", markTakenPendingIntent)
            .addAction(R.drawable.ic_snooze, "Postpone", postponePendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}