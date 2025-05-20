package com.example.medireminder.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medireminder.R
import com.example.medireminder.data.MedicationDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private lateinit var viewModel: MedicationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewHistory)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = HistoryAdapter { medication ->
            lifecycleScope.launch {
                viewModel.delete(medication)
                Log.d("HistoryFragment", "Deleted medication: ${medication.id}")
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        val dao = MedicationDatabase.getDatabase(requireContext()).medicationDao()
        viewModel = ViewModelProvider(this, MedicationViewModelFactory(dao))
            .get(MedicationViewModel::class.java)

        lifecycleScope.launch {
            viewModel.getMedicationsByStatus(true).collectLatest { medications ->
                Log.d("HistoryFragment", "Received medications: ${medications.map { it.id }}")
                adapter.submitList(medications)
            }
        }
    }
}