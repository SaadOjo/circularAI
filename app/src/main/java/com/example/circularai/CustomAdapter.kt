package com.example.circularai

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

class CustomAdapter(private val dataset: List<Map<String, Any?>>):RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val date_tv: TextView
        val time_tv: TextView
        val plastic_tv: TextView
        val metal_tv: TextView
        val glass_tv: TextView
        val paper_tv: TextView
        val total_tv: TextView

        init {
            date_tv = view.findViewById(R.id.recycle_date_tv)
            time_tv = view.findViewById(R.id.recycle_time_tv)
            plastic_tv = view.findViewById(R.id.plastic_point_tv)
            metal_tv = view.findViewById(R.id.metal_point_tv)
            glass_tv = view.findViewById(R.id.glass_point_tv)
            paper_tv = view.findViewById(R.id.paper_point_tv)
            total_tv = view.findViewById(R.id.total_point_tv)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycle_history_element, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data_map = dataset[position]
        Log.i("INFO", data_map.toString())
        //val data = recycled_entry(data_map["epoch"] as Long?, data_map["plastic"] as Int?, data_map["metal"] as Int?, data_map["paper"] as Int?, data_map["glass"] as Int?  )
        val ldt = LocalDateTime.ofEpochSecond(data_map["epoch"]!! as Long, 0, ZoneOffset.UTC)
        val date_formatter = DateTimeFormatter.ofPattern("dd MMM yy")
        val time_formatter = DateTimeFormatter.ofPattern("hh:mm")
        val date = ldt?.format(date_formatter)
        val time = ldt?.format(time_formatter)
        holder.date_tv.text = date
        holder.time_tv.text = time
        holder.plastic_tv.text = data_map["plastic"].toString()
        holder.metal_tv.text = data_map["metal"].toString()
        holder.glass_tv.text = data_map["glass"].toString()
        holder.paper_tv.text = data_map["glass"].toString()
        holder.total_tv.text = (data_map["plastic"] as Long + data_map["metal"] as Long + data_map["glass"] as Long + data_map["glass"] as Long ).toString()
    }

    override fun getItemCount(): Int {
        return dataset.size
    }
}