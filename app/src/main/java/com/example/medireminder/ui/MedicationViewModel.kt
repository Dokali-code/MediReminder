package com.example.medireminder.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.medireminder.data.Medication
import com.example.medireminder.data.MedicationDao
import kotlinx.coroutines.flow.Flow

class MedicationViewModel(private val medicationDao: MedicationDao) : ViewModel() {
    fun getMedicationsByStatus(isTaken: Boolean): Flow<List<Medication>> {
        return medicationDao.getMedicationsByStatus(isTaken)
    }
    suspend fun markAsTaken(medication: Medication) {
        val updated = medication.copy(isTaken = true, takenTime = System.currentTimeMillis())
        medicationDao.update(updated)
        Log.d("MedicationViewModel", "Updated: ${updated.isTaken}")
        val fetched = medicationDao.getMedicationById(updated.id)
        Log.d("MedicationViewModel", "Fetched after update: ${fetched?.isTaken}")
    }
    suspend fun delete(medication: Medication) {
        medicationDao.delete(medication)
    }
}