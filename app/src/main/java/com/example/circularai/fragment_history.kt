package com.example.circularai

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDateTime


class fragment_history : Fragment(R.layout.fragment_history) {

    private lateinit var recycler_view: RecyclerView
    private lateinit var user:FirebaseUser
    private lateinit var db: FirebaseDatabase

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recycler_view = view.findViewById(R.id.rv)
        user = FirebaseAuth.getInstance().currentUser!!
        db  = FirebaseDatabase.getInstance("https://arcelik-recycling-default-rtdb.firebaseio.com")
        db.getReference("users/" + user.uid).get().addOnSuccessListener {
            val value = it.value
            if(value != null){
                //Log.i("INFO", value as )
                val dataset = value as List<Map<String, Any?>>
                //Log.i("INFO", dataset.toString())
                //dataset.forEach({d2.add(it)})
                recycler_view.adapter = CustomAdapter(dataset)
                recycler_view.layoutManager = LinearLayoutManager(requireActivity())
            }
        }.addOnFailureListener({
            Log.i("INFO", "Failed to get the list")
        })


    }
}