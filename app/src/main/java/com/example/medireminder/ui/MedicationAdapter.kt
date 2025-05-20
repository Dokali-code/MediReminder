package com.example.medireminder.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medireminder.R
import com.example.medireminder.data.Medication

class MedicationAdapter(
    private val onMarkAsTaken: (Medication) -> Unit = {},
    private val onDelete: (Medication) -> Unit = {},
    private val onUpdate: (Medication) -> Unit = {}
) : ListAdapter<Medication, MedicationAdapter.MedicationViewHolder>(MedicationDiffCallback()) {

    class MedicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tvMedicationName)
        private val dosageTextView: TextView = itemView.findViewById(R.id.tvDosage)
        private val timeTextView: TextView = itemView.findViewById(R.id.tvSelectedTime)
        private val updateButton: Button = itemView.findViewById(R.id.btnUpdate)
        private val deleteButton: Button? = itemView.findViewById(R.id.btnDelete)

        fun bind(medication: Medication, onMarkAsTaken: (Medication) -> Unit, onDelete: (Medication) -> Unit, onUpdate: (Medication) -> Unit) {
            nameTextView.text = medication.name
            dosageTextView.text = medication.dosage
            timeTextView.text = medication.timeToTake
            updateButton.text = itemView.context.getString(R.string.update_button)
            deleteButton?.text = itemView.context.getString(R.string.delete_button)
            itemView.setOnClickListener { onMarkAsTaken(medication) }
            updateButton.setOnClickListener { onUpdate(medication) }
            deleteButton?.apply {
                visibility = if (medication.isTaken) View.GONE else View.VISIBLE
                setOnClickListener { onDelete(medication) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medication, parent, false)
        return MedicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicationViewHolder, position: Int) {
        holder.bind(getItem(position), onMarkAsTaken, onDelete, onUpdate)
    }
}

class MedicationDiffCallback : DiffUtil.ItemCallback<Medication>() {
    override fun areItemsTheSame(oldItem: Medication, newItem: Medication): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Medication, newItem: Medication): Boolean {
        return oldItem == newItem
    }
}