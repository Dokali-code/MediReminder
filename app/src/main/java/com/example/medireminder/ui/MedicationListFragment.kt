package com.example.medireminder.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medireminder.R
import com.example.medireminder.data.MedicationDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MedicationListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MedicationAdapter
    private lateinit var viewModel: MedicationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_medication_list, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MedicationAdapter(
            onMarkAsTaken = { medication ->
                lifecycleScope.launch {
                    viewModel.markAsTaken(medication)
                }
            },
            onDelete = { medication ->
                AlertDialog.Builder(requireContext())
                    .setMessage(getString(R.string.delete_confirmation, medication.name))
                    .setPositiveButton(R.string.delete_button) { _, _ ->
                        lifecycleScope.launch {
                            viewModel.delete(medication)
                        }
                    }
                    .setNegativeButton(R.string.cancel_button) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            },
            onUpdate = { medication ->
                val intent = Intent(requireContext(), FormActivity::class.java).apply {
                    putExtra("medicationId", medication.id)
                }
                startActivity(intent)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        val dao = MedicationDatabase.getDatabase(requireContext()).medicationDao()
        viewModel = ViewModelProvider(requireActivity(), MedicationViewModelFactory(dao))
            .get(MedicationViewModel::class.java)

        lifecycleScope.launch {
            viewModel.getMedicationsByStatus(false).collectLatest { medications ->
                adapter.submitList(medications)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            viewModel.getMedicationsByStatus(false).collectLatest { medications ->
                adapter.submitList(medications)
            }
        }
    }
}