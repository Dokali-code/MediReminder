package com.example.medireminder.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.form)

        nameEditText = findViewById(R.id.editTextText)
        dosageEditText = findViewById(R.id.editTextText3)
        noteEditText = findViewById(R.id.editTextText4)
        tvSelectedTime = findViewById(R.id.tvSelectedTime)
        btnTimePicker = findViewById(R.id.btnTimePicker)
        addButton = findViewById(R.id.button)

        val medicationDao = MedicationDatabase.getDatabase(this).medicationDao()

        btnTimePicker.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, hourOfDay, minuteOfHour ->
                selectedTime = String.format("%02d:%02d", hourOfDay, minuteOfHour)
                tvSelectedTime.text = selectedTime
            }, hour, minute, true).show()
        }

        addButton.setOnClickListener {
            addButton.isEnabled = false // Prevent multiple clicks
            val name = nameEditText.text.toString().trim()
            val dosage = dosageEditText.text.toString().trim()
            val note = noteEditText.text.toString().trim()

            if (name.isEmpty() || dosage.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(this, "Name, dosage, and time are required", Toast.LENGTH_SHORT).show()
                addButton.isEnabled = true
                return@setOnClickListener
            }

            val medication = Medication(
                name = name,
                dosage = dosage,
                timeToTake = selectedTime,
                note = if (note.isEmpty()) null else note
            )

            lifecycleScope.launch(Dispatchers.IO) {
                val medicationId = medicationDao.insert(medication) // Insert only once
                scheduleReminder(medicationId, selectedTime)
                launch(Dispatchers.Main) {
                    Toast.makeText(this@FormActivity, "Medication saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun scheduleReminder(medicationId: Long, timeToTake: String) {
        val (hour, minute) = timeToTake.split(":").map { it.toInt() }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1) // Schedule for next day if time has passed
            }
        }

        val delay = calendar.timeInMillis - System.currentTimeMillis()
        val data = Data.Builder().putInt("medicationId", medicationId.toInt()).build()

        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }
}