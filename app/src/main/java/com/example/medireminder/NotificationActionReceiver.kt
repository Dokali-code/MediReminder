package com.example.medireminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.example.medireminder.data.Medication
import com.example.medireminder.data.MedicationDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.medireminder.workers.MedicationReminderWorker
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getIntExtra("medicationId", -1)
        val action = intent.action

        if (medicationId == -1) return

        val dao = MedicationDatabase.getDatabase(context).medicationDao()

        (context as? AppCompatActivity)?.lifecycleScope?.launch(Dispatchers.IO) {
            val medication = dao.getMedicationById(medicationId) ?: return@launch
            when (action) {
                "ACTION_MARK_TAKEN" -> {
                    dao.update(
                        medication.copy(
                            isTaken = true,
                            takenTime = System.currentTimeMillis()
                        )
                    )
                    cancelNotification(context)
                }
                "ACTION_POSTPONE" -> {
                    // Reschedule for 15 minutes later
                    scheduleReminder(context, medicationId, medication.timeToTake, 15 * 60 * 1000L)
                    cancelNotification(context)
                }
            }
        }
    }

    private fun cancelNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(MedicationReminderWorker.NOTIFICATION_ID)
    }

    private fun scheduleReminder(context: Context, medicationId: Int, timeToTake: String, delay: Long) {
        val data = Data.Builder().putInt("medicationId", medicationId).build()
        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}