package com.example.medireminder.ui

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
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
import java.util.Locale
import java.util.concurrent.TimeUnit

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val dao = MedicationDatabase.getDatabase(requireContext()).medicationDao()

        // Notification toggle
        findPreference<SwitchPreferenceCompat>("notifications_enabled")?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == false) {
                WorkManager.getInstance(requireContext()).cancelAllWork()
                Toast.makeText(requireContext(), R.string.notifications_disabled, Toast.LENGTH_SHORT).show()
            } else {
                rescheduleAllReminders()
                Toast.makeText(requireContext(), R.string.notifications_enabled, Toast.LENGTH_SHORT).show()
            }
            true
        }

        // Clear history
        findPreference<Preference>("clear_history")?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.clear_history_confirmation)
                .setPositiveButton(R.string.clear_button) { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        dao.deleteAllTaken()
                        launch(Dispatchers.Main) {
                            Toast.makeText(requireContext(), R.string.history_cleared, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton(R.string.cancel_button, null)
                .show()
            true
        }

        // Clear all medications
        findPreference<Preference>("clear_all_medications")?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.clear_all_medications_confirmation)
                .setPositiveButton(R.string.delete_button) { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        dao.deleteAll()
                        WorkManager.getInstance(requireContext()).cancelAllWork()
                        launch(Dispatchers.Main) {
                            Toast.makeText(requireContext(), R.string.all_medications_cleared, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton(R.string.cancel_button, null)
                .show()
            true
        }

        // Reset reminders
        findPreference<Preference>("reset_reminders")?.setOnPreferenceClickListener {
            rescheduleAllReminders()
            Toast.makeText(requireContext(), R.string.reminders_reset, Toast.LENGTH_SHORT).show()
            true
        }

        // Language change
        findPreference<ListPreference>("language")?.setOnPreferenceChangeListener { _, newValue ->
            val language = newValue as String
            val locale = Locale(language)
            Locale.setDefault(locale)
            val config = Configuration(resources.configuration).apply {
                setLocale(locale)
            }
            resources.updateConfiguration(config, resources.displayMetrics)
            requireActivity().recreate() // Recreate activity to apply new locale
            true
        }
    }

    private fun rescheduleAllReminders() {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = MedicationDatabase.getDatabase(requireContext()).medicationDao()
            val medications = dao.getAllMedicationsSync()
            medications.forEach { medication ->
                if (!medication.isTaken) {
                    val timeParts = medication.timeToTake.split(":").map { it.toIntOrNull() ?: 0 }
                    if (timeParts.size != 2) return@forEach // Skip invalid time format
                    val (hour, minute) = timeParts
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        if (before(Calendar.getInstance())) {
                            add(Calendar.DAY_OF_MONTH, 1)
                        }
                    }
                    val delay = calendar.timeInMillis - System.currentTimeMillis()
                    if (delay < 0) return@forEach // Skip invalid delays
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