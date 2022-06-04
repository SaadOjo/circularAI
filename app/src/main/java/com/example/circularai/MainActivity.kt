package com.example.circularai

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.circularai.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding;

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(getLayoutInflater())
        setContentView(binding.root)

        val f1 = fragment_1()
        val f2 = fragment_2()

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment, f1)
            commit()
        }

        val nav_view = binding.bottomNavigationView;

        nav_view.setOnItemSelectedListener { item ->
            when (item.getItemId()) {
                R.id.nav_camera -> replaceFragment(f1)
                R.id.nav_map -> replaceFragment(f2)
                else -> {
                    replaceFragment(f1)
                }
            }
        }

    }

    fun replaceFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment, fragment)
            addToBackStack(null)
            commit()
            return true
        }
    }
}