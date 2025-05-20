package com.example.medireminder.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.medireminder.R
import com.example.medireminder.data.Medication
import com.example.medireminder.data.MedicationDatabase
import com.example.medireminder.workers.MedicationReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class FormActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var dosageEditText: EditText
    private lateinit var noteEditText: EditText
    private lateinit var tvSelectedTime: TextView
    private lateinit var btnTimePicker: ImageButton
    private lateinit var addButton: Button

    private var selectedTime: String = ""
    private var medicationId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply language from preferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val language = prefs.getString("language", "en") ?: "en"
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.form)

        nameEditText = findViewById(R.id.editTextText)
        dosageEditText = findViewById(R.id.editTextText3)
        noteEditText = findViewById(R.id.editTextText4)
        tvSelectedTime = findViewById(R.id.tvSelectedTime)
        btnTimePicker = findViewById(R.id.btnTimePicker)
        addButton = findViewById(R.id.button)

        val medicationDao = MedicationDatabase.getDatabase(this).medicationDao()

        // Check if editing an existing medication
        medicationId = intent.getIntExtra("medicationId", -1)
        if (medicationId != -1) {
            lifecycleScope.launch(Dispatchers.IO) {
                val medication = medicationDao.getMedicationById(medicationId)
                launch(Dispatchers.Main) {
                    medication?.let {
                        nameEditText.setText(it.name)
                        dosageEditText.setText(it.dosage)
                        noteEditText.setText(it.note ?: "")
                        tvSelectedTime.text = it.timeToTake
                        selectedTime = it.timeToTake
                        addButton.text = getString(R.string.update_button)
                    } ?: run {
                        Toast.makeText(this@FormActivity, R.string.medication_not_found, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }

        btnTimePicker.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, hourOfDay, minuteOfHour ->
                selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour)
                tvSelectedTime.text = selectedTime
            }, hour, minute, true).show()
        }

        addButton.setOnClickListener {
            addButton.isEnabled = false // Prevent multiple clicks
            val name = nameEditText.text.toString().trim()
            val dosage = dosageEditText.text.toString().trim()
            val note = noteEditText.text.toString().trim()

            if (name.isEmpty() || dosage.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(this, getString(R.string.fields_required), Toast.LENGTH_SHORT).show()
                addButton.isEnabled = true
                return@setOnClickListener
            }

            val medication = Medication(
                id = if (medicationId != -1) medicationId else 0,
                name = name,
                dosage = dosage,
                timeToTake = selectedTime,
                note = if (note.isEmpty()) null else note
            )

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    if (medicationId != -1) {
                        medicationDao.update(medication)
                        scheduleReminder(medicationId.toLong(), selectedTime)
                        launch(Dispatchers.Main) {
                            Toast.makeText(this@FormActivity, getString(R.string.medication_updated), Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } else {
                        val newMedicationId = medicationDao.insert(medication)
                        scheduleReminder(newMedicationId, selectedTime)
                        launch(Dispatchers.Main) {
                            Toast.makeText(this@FormActivity, getString(R.string.medication_saved), Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@FormActivity, R.string.error_saving_medication, Toast.LENGTH_SHORT).show()
                        addButton.isEnabled = true
                    }
                }
            }
        }
    }

    private fun scheduleReminder(medicationId: Long, timeToTake: String) {
        val (hour, minute) = timeToTake.split(":").map { it.toIntOrNull() ?: 0 }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1) // Schedule for next day if time has passed
            }
        }

        val delay = calendar.timeInMillis - System.currentTimeMillis()
        if (delay < 0) return // Skip invalid delays

        val data = Data.Builder().putInt("medicationId", medicationId.toInt()).build()

        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }
}