package com.example.medireminder.ui

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.medireminder.R
import com.example.medireminder.data.MedicationDatabase
import com.example.medireminder.workers.MedicationReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val dao = MedicationDatabase.getDatabase(requireContext()).medicationDao()

        // Notification toggle
        findPreference<SwitchPreferenceCompat>("notifications_enabled")?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == false) {
                WorkManager.getInstance(requireContext()).cancelAllWork()
            } else {
                rescheduleAllReminders()
            }
            true
        }

        // Clear history
        findPreference<Preference>("clear_history")?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage("Clear all history?")
                .setPositiveButton("Clear") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        dao.deleteAllTaken()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }

        // Clear all medications
        findPreference<Preference>("clear_all_medications")?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage("Delete all medications? This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        dao.deleteAll()
                        WorkManager.getInstance(requireContext()).cancelAllWork()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }

        // Reset reminders
        findPreference<Preference>("reset_reminders")?.setOnPreferenceClickListener {
            rescheduleAllReminders()
            true
        }
    }

    private fun rescheduleAllReminders() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = MedicationDatabase.getDatabase(requireContext()).medicationDao()
            val medications = dao.getAllMedicationsSync()
            medications.forEach { medication ->
                if (!medication.isTaken) {
                    val (hour, minute) = medication.timeToTake.split(":").map { it.toInt() }
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        if (before(Calendar.getInstance())) {
                            add(Calendar.DAY_OF_MONTH, 1)
                        }
                    }
                    val delay = calendar.timeInMillis - System.currentTimeMillis()
                    val data = Data.Builder().putInt("medicationId", medication.id).build()
                    val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(data)
                        .build()
                    WorkManager.getInstance(requireContext()).enqueueUniqueWork(
                        "reminder_${medication.id}",
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
                }
            }
        }
    }
}