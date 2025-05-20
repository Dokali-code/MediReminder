package com.example.medireminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.medireminder.data.Medication
import com.example.medireminder.data.MedicationDatabase
import com.example.medireminder.workers.MedicationReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getIntExtra("medicationId", -1)
        val action = intent.action

        if (medicationId == -1) {
            Log.e("NotificationActionReceiver", "Invalid medicationId received")
            return
        }

        val dao = MedicationDatabase.getDatabase(context).medicationDao()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val medication = dao.getMedicationById(medicationId) ?: run {
                    Log.e("NotificationActionReceiver", "Medication not found for ID: $medicationId")
                    return@launch
                }
                when (action) {
                    "ACTION_MARK_TAKEN" -> {
                        val updatedMedication = medication.copy(
                            isTaken = true,
                            takenTime = System.currentTimeMillis()
                        )
                        dao.update(updatedMedication)
                        Log.d("NotificationActionReceiver", "Updated medication: ${updatedMedication.id}, isTaken: ${updatedMedication.isTaken}")
                        cancelNotification(context, medicationId)
                    }
                    "ACTION_POSTPONE" -> {
                        scheduleReminder(context, medicationId, 5 * 60 * 1000L)
                        cancelNotification(context, medicationId)
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationActionReceiver", "Error handling action: $action", e)
            }
        }
    }

    private fun cancelNotification(context: Context, medicationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(medicationId)
        Log.d("NotificationActionReceiver", "Cancelled notification for medicationId: $medicationId")
    }

    private fun scheduleReminder(context: Context, medicationId: Int, delay: Long) {
        val data = Data.Builder().putInt("medicationId", medicationId).build()
        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("reminder_$medicationId", ExistingWorkPolicy.REPLACE, workRequest)
        Log.d("NotificationActionReceiver", "Scheduled reminder for medicationId: $medicationId with delay: $delay ms")
    }
}