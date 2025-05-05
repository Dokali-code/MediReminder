package com.example.medireminder.ui

import androidx.lifecycle.ViewModel
import com.example.medireminder.data.Medication
import com.example.medireminder.data.MedicationDao
import kotlinx.coroutines.flow.Flow

class MedicationViewModel(private val medicationDao: MedicationDao) : ViewModel() {
    fun getMedicationsByStatus(isTaken: Boolean): Flow<List<Medication>> {
        return medicationDao.getMedicationsByStatus(isTaken)
    }

    suspend fun markAsTaken(medication: Medication) {
        medicationDao.update(
            medication.copy(
                isTaken = true,
                takenTime = System.currentTimeMillis()
            )
        )
    }
    suspend fun delete(medication: Medication) {
        medicationDao.delete(medication)
    }
}