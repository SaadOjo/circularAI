package com.example.circularai

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.setFragmentResultListener


class RecyclingFragment(key:String) : Fragment(R.layout.fragment_recycling) {

    var key = key
    lateinit var title_tv:TextView
    lateinit var e1_tv:TextView
    lateinit var e2_tv:TextView
    lateinit var e3_tv:TextView
    lateinit var elements_list: List<TextView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title_tv = view.findViewById(R.id.type_tv)
        e1_tv = view.findViewById(R.id.element_1_tv)
        e2_tv = view.findViewById(R.id.element_2_tv)
        e3_tv = view.findViewById(R.id.element_3_tv)

        elements_list = arrayListOf(e1_tv, e2_tv, e3_tv).toList()

        setType(key)


        setFragmentResultListener(key) { requestKey, bundle ->

            val list_result = bundle.get("list")
            if (list_result != null) {
                setElements(list_result as List<String>)
            }
        }


    }

    private fun setType(type:String){
        var color:Int = Color.WHITE
        title_tv.text = type.capitalize()
        Log.i("RECYCLING_FRAGMENT",  "$type")

        when(type){
            "glass" -> color = resources.getColor(R.color.Glass)
            "paper" -> color = resources.getColor(R.color.Paper)
            "metal" -> color = resources.getColor(R.color.Metal)
            "plastic" -> color = resources.getColor(R.color.Plastic)
            else -> {}

        }
        title_tv.setBackgroundColor(color)
    }

    private fun setElements(list:List<String>){
        var last_index: Int = -1
        var filtered_list:List<String> = list
        if(list.size > 3){
            filtered_list = list.take(3)
        }
        filtered_list.forEachIndexed { index, s ->
            elements_list[index].text = s.capitalize()
            last_index = index
        }
        ((last_index + 1)..2).forEach{elements_list[it].text = ""}

    }

}