package com.example.medireminder.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medireminder.R
import com.example.medireminder.data.Medication

class HistoryAdapter(
    private val onRemove: (Medication) -> Unit
) : ListAdapter<Medication, HistoryAdapter.HistoryViewHolder>(MedicationDiffCallback()) {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tvMedicationName)
        private val timeToTakeTextView: TextView = itemView.findViewById(R.id.tvTimeToTake)
        private val removeButton: Button = itemView.findViewById(R.id.btnRemove)

        fun bind(medication: Medication, onRemove: (Medication) -> Unit) {
            Log.d("HistoryAdapter", "Binding medication: ${medication.name}")
            nameTextView.text = medication.name
            timeToTakeTextView.text = "Time to take: ${medication.timeToTake}"
            removeButton.setOnClickListener { onRemove(medication) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position), onRemove)
    }
}