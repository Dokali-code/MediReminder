package com.example.medireminder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.medireminder.data.MedicationDao

class MedicationViewModelFactory(private val medicationDao: MedicationDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicationViewModel(medicationDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}