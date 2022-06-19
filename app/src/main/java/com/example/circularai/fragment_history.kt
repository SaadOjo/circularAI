package com.example.circularai

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class fragment_history : Fragment(R.layout.fragment_history) {

    private lateinit var recycler_view: RecyclerView
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recycler_view = view.findViewById(R.id.rv)
        val dataset = mutableListOf(
            recycled_entry("apple"),
            recycled_entry("bravo"),
            recycled_entry("copy")
        )
        recycler_view.adapter = CustomAdapter(dataset)
        recycler_view.layoutManager = LinearLayoutManager(requireActivity())

    }
}