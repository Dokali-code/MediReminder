package com.example.medireminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val timeToTake: String,
    val note: String? = null,
    val isTaken: Boolean = false,
    val takenTime: Long? = null
)