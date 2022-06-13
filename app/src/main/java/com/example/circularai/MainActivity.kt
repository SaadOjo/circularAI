package com.example.circularai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.circularai.databinding.ActivityMainBinding
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private val permissions = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA)
    private val permissionsRequestCode = Random.nextInt(0, 10000)

    lateinit var binding: ActivityMainBinding;

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(getLayoutInflater())
        setContentView(binding.root)

        val f1 = detector_fragment()
        val f2 = map_fragment()
        val f3 = debug_fragment()

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment, f1)
            commit()
        }

        val nav_view = binding.bottomNavigationView;

        nav_view.setOnItemSelectedListener { item ->
            when (item.getItemId()) {
                R.id.nav_camera -> replaceFragment(f1)
                R.id.nav_map -> replaceFragment(f2)
                R.id.nav_debug -> replaceFragment(f3)

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
    override fun onResume() {
        super.onResume()

        // Request permissions each time the app resumes, since they can be revoked at any time
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), permissionsRequestCode)
        } else {
            start_gps()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && hasPermissions(this)) {
            start_gps()
        } else {
            finish() // If we don't have the required permissions, we can't run
        }
    }

    /** Convenience method used to check if all permissions required by this app are granted */
    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun start_gps(): Boolean {
        Toast.makeText(this, "Permissions have been granted", Toast.LENGTH_LONG).show()
        return true
    }
}